package com.trip.attraction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.attraction.client.AttractionTourApiClient;
import com.trip.attraction.dto.AttractionSearchRequestDto;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.dto.AttractionTourApiResponse.AreaItem;
import com.trip.attraction.entity.Attraction;
import com.trip.attraction.repository.AttractionRepository;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.AttractionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeMap;

/**
 * 관광지 검색·상세·지역코드 서비스.
 * TourAPI 호출 결과를 Redis에 캐시하며, 실패 시 stale 캐시를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttractionService {

    private static final Duration TTL_SEARCH = Duration.ofMinutes(10);
    private static final Duration TTL_DETAIL = Duration.ofHours(6);
    private static final Duration TTL_AREAS  = Duration.ofHours(24);

    private static final String PREFIX_SEARCH = "cache:attr:search:";
    private static final String PREFIX_DETAIL = "cache:attr:detail:";
    private static final String KEY_AREAS     = "cache:attr:areas";

    private final AttractionTourApiClient tourApiClient;
    private final AttractionRepository    attractionRepository;
    private final StringRedisTemplate     stringRedisTemplate;
    private final ObjectMapper            objectMapper;

    // ─────────────────────────────────────────────────────────────
    // 검색 (areaBasedList2 / searchKeyword2)
    // ─────────────────────────────────────────────────────────────

    public List<AttractionItem> search(AttractionSearchRequestDto req) {
        // keyword 유효성 검사
        String keyword = req.keyword() == null ? "" : req.keyword().trim();
        if (!keyword.isEmpty() && keyword.length() < 2) {
            throw new AttractionHandler(ResponseCode.INVALID_KEYWORD);
        }

        int size = req.clampedSize();
        int page = req.page() <= 0 ? 1 : req.page();

        // 캐시 키 생성 — 파라미터 정규화 후 SHA-256
        String cacheKey = PREFIX_SEARCH + buildSearchHash(keyword, req, size, page);

        // 1) 캐시 히트
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserializeItems(cached);
        }

        // 2) TourAPI 호출
        try {
            List<AttractionItem> items;
            if (!keyword.isEmpty()) {
                items = tourApiClient.fetchByKeyword(keyword, req.areaCode(), req.sigunguCode(), req.contentTypeId(), page, size);
            } else if (req.hasCoords()) {
                // 내 위치 근처 — 좌표 기반 거리순 조회
                items = tourApiClient.fetchLocationBased(req.mapX(), req.mapY(), req.clampedRadius(), req.contentTypeId(), page, size);
            } else {
                items = tourApiClient.fetchAreaBased(req.areaCode(), req.sigunguCode(), req.contentTypeId(), page, size);
            }

            String json = serializeItems(items);
            stringRedisTemplate.opsForValue().set(cacheKey, json, TTL_SEARCH);
            return items;

        } catch (Exception e) {
            log.warn("TourAPI 검색 실패 — stale 캐시 반환 시도. key={}, error={}", cacheKey, e.getMessage());
            // stale 재시도 (TTL 만료 직전 캐시가 있을 수 있음 — 이미 체크했지만 재확인)
            String stale = stringRedisTemplate.opsForValue().get(cacheKey);
            if (stale != null) {
                return deserializeItems(stale);
            }
            throw new AttractionHandler(ResponseCode.EXTERNAL_API_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 상세 조회 (detailCommon2)
    // ─────────────────────────────────────────────────────────────

    public AttractionItem getDetail(String contentId) {
        String cacheKey = PREFIX_DETAIL + contentId;

        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserializeItem(cached);
        }

        try {
            AttractionItem item = tourApiClient.fetchDetail(contentId);
            if (item == null) {
                throw new AttractionHandler(ResponseCode.ATTRACTION_NOT_FOUND);
            }
            stringRedisTemplate.opsForValue().set(cacheKey, serializeItem(item), TTL_DETAIL);
            return item;

        } catch (AttractionHandler e) {
            throw e;
        } catch (Exception e) {
            log.warn("TourAPI 상세 조회 실패 — stale 캐시 반환 시도. contentId={}, error={}", contentId, e.getMessage());
            String stale = stringRedisTemplate.opsForValue().get(cacheKey);
            if (stale != null) {
                return deserializeItem(stale);
            }
            throw new AttractionHandler(ResponseCode.EXTERNAL_API_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 지역코드 목록 (areaCode2)
    // ─────────────────────────────────────────────────────────────

    public List<AreaItem> getAreas() {
        String cached = stringRedisTemplate.opsForValue().get(KEY_AREAS);
        if (cached != null) {
            return deserializeAreaItems(cached);
        }

        try {
            List<AreaItem> areas = tourApiClient.fetchAreaCodes(null);
            stringRedisTemplate.opsForValue().set(KEY_AREAS, serializeAreaItems(areas), TTL_AREAS);
            return areas;

        } catch (Exception e) {
            log.warn("TourAPI 지역코드 조회 실패 — stale 캐시 반환 시도. error={}", e.getMessage());
            String stale = stringRedisTemplate.opsForValue().get(KEY_AREAS);
            if (stale != null) {
                return deserializeAreaItems(stale);
            }
            throw new AttractionHandler(ResponseCode.EXTERNAL_API_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // upsertSnapshot — plan 도메인이 장소 담기 시 사용
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Attraction upsertSnapshot(String contentId, Integer contentType) {
        AttractionItem item = tourApiClient.fetchDetail(contentId);
        if (item == null) {
            throw new AttractionHandler(ResponseCode.ATTRACTION_NOT_FOUND);
        }
        return upsertFromItem(contentId, contentType, item);
    }

    /**
     * contentType 힌트 없이 스냅샷을 upsert한다 (AI 초안 변환 등).
     * detailCommon2 응답의 contentTypeId로 실제 유형을 추론하고,
     * 응답에 없으면 12(관광지)로 폴백한다.
     */
    @Transactional
    public Attraction upsertSnapshot(String contentId) {
        AttractionItem item = tourApiClient.fetchDetail(contentId);
        if (item == null) {
            throw new AttractionHandler(ResponseCode.ATTRACTION_NOT_FOUND);
        }
        Integer contentType = parseInt(item.contentTypeId());
        if (contentType == null) {
            contentType = 12; // detailCommon2가 유형을 주지 않으면 관광지로 폴백
        }
        return upsertFromItem(contentId, contentType, item);
    }

    /**
     * detailCommon2 응답(item)으로 (contentId, contentType) 스냅샷을 갱신/생성한다.
     * 좌표(mapx/mapy)가 응답에 없으면 기존 스냅샷 좌표를 보존해 null 덮어쓰기를 막는다.
     */
    private Attraction upsertFromItem(String contentId, Integer contentType, AttractionItem item) {
        Double lat = parseDouble(item.mapy());   // 위도
        Double lng = parseDouble(item.mapx());   // 경도

        return attractionRepository.findByContentIdAndContentType(contentId, contentType)
                .map(existing -> {
                    // detailCommon2가 좌표를 반환하지 않으면 기존 좌표 유지 (null 덮어쓰기 방지)
                    Double newLat = lat != null ? lat : existing.getLatitude();
                    Double newLng = lng != null ? lng : existing.getLongitude();
                    existing.update(
                            item.title(),
                            item.addr1(),
                            item.areaCode(),
                            item.sigunguCode(),
                            newLat,
                            newLng,
                            item.firstimage(),
                            item.tel(),
                            item.overview()
                    );
                    return existing;
                })
                .orElseGet(() -> {
                    try {
                        return attractionRepository.save(Attraction.builder()
                                .contentId(contentId)
                                .contentType(contentType)
                                .title(item.title())
                                .addr(item.addr1())
                                .areaCode(item.areaCode())
                                .sigunguCode(item.sigunguCode())
                                .latitude(lat)
                                .longitude(lng)
                                .imageUrl(item.firstimage())
                                .tel(item.tel())
                                .overview(item.overview())
                                .fetchedAt(LocalDateTime.now())
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 insert 충돌(uk_attr_content) — 이미 저장된 레코드 반환
                        return attractionRepository.findByContentIdAndContentType(contentId, contentType)
                                .orElseThrow(() -> new AttractionHandler(ResponseCode.ATTRACTION_NOT_FOUND));
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────────────────────

    /**
     * 검색 파라미터를 정규화한 뒤 SHA-256 해시를 반환.
     * keyword: trim → lower → 공백 압축. 나머지는 TreeMap 정렬.
     */
    private String buildSearchHash(String normalizedKeyword, AttractionSearchRequestDto req,
                                   int size, int page) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("keyword",       normalizedKeyword.toLowerCase().replaceAll("\\s+", " "));
        params.put("areaCode",      nvl(req.areaCode()));
        params.put("sigunguCode",   nvl(req.sigunguCode()));
        params.put("contentTypeId", nvl(req.contentTypeId()));
        params.put("size",          String.valueOf(size));
        params.put("page",          String.valueOf(page));
        params.put("mapX",          nvl(req.mapX()));
        params.put("mapY",          nvl(req.mapY()));
        params.put("radius",        req.hasCoords() ? String.valueOf(req.clampedRadius()) : "");

        String raw = params.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    // JSON 직렬화/역직렬화 — StringRedisTemplate 사용이므로 수동 변환
    private String serializeItems(List<AttractionItem> items) {
        try { return objectMapper.writeValueAsString(items); }
        catch (Exception e) { throw new IllegalStateException("Redis 직렬화 실패", e); }
    }

    private List<AttractionItem> deserializeItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AttractionItem>>() {});
        } catch (Exception e) {
            log.warn("Redis 역직렬화 실패 — 캐시 무시. error={}", e.getMessage());
            return List.of();
        }
    }

    private String serializeItem(AttractionItem item) {
        try { return objectMapper.writeValueAsString(item); }
        catch (Exception e) { throw new IllegalStateException("Redis 직렬화 실패", e); }
    }

    private AttractionItem deserializeItem(String json) {
        try {
            return objectMapper.readValue(json, AttractionItem.class);
        } catch (Exception e) {
            log.warn("Redis 역직렬화 실패 — 캐시 무시. error={}", e.getMessage());
            return null;
        }
    }

    private String serializeAreaItems(List<AreaItem> items) {
        try { return objectMapper.writeValueAsString(items); }
        catch (Exception e) { throw new IllegalStateException("Redis 직렬화 실패", e); }
    }

    private List<AreaItem> deserializeAreaItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AreaItem>>() {});
        } catch (Exception e) {
            log.warn("Redis AreaItem 역직렬화 실패 — 캐시 무시. error={}", e.getMessage());
            return List.of();
        }
    }
}
