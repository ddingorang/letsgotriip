package com.trip.attraction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.attraction.client.AttractionTourApiClient;
import com.trip.attraction.dto.AttractionSearchRequestDto;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.dto.AttractionTourApiResponse.AreaItem;
import com.trip.attraction.entity.Attraction;
import com.trip.attraction.repository.AttractionRepository;
import com.trip.global.config.CacheConfig;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.AttractionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    // ─────────────────────────────────────────────────────────────
    // 이중 캐시 구조 (역할 구분)
    //  1) @Cacheable(CacheManager) — 1차 캐시. 정상 응답을 짧은 TTL로 보관해 외부 왕복을 줄인다.
    //     빈 결과는 unless로 캐시하지 않는다(CacheConfig 참조).
    //  2) 수동 stringRedisTemplate — stale 폴백 캐시. 외부 API 장애 시 만료 직전 응답을 반환한다.
    //     폴백이 의미를 가지려면 stale TTL이 1차(@Cacheable) TTL "이상"이어야 한다
    //     (1차 만료 → 외부 호출 → 실패 시 stale로 복구). 그래서 검색은 1차(15분)와 동일,
    //     상세는 1차(30분)보다 길게 잡아 더 오래 폴백되도록 한다.
    //  ※ stale 캐시에는 빈 결과를 저장하지 않는다(#19). 빈 결과를 stale로 굳히면
    //     일시적 빈 응답이 장애 시 그대로 폴백되어 혼동을 부른다.
    // ─────────────────────────────────────────────────────────────

    // 1차 @Cacheable(attractions, 15분)과 동일하게 맞춰 TTL 의미를 정합화(#21).
    private static final Duration TTL_SEARCH = Duration.ofMinutes(15);
    // 1차 @Cacheable(attractionDetail, 30분)보다 길게 — 상세는 변동이 적어 폴백을 더 오래 허용.
    private static final Duration TTL_DETAIL = Duration.ofHours(6);
    // 1차 @Cacheable(attractionAreas, 24시간)과 동일.
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

    /**
     * 지역/키워드/위치 기반 검색.
     * 외부 TourAPI 왕복을 Redis("attractions", 15분)에 캐시한다.
     * 캐시 키는 모든 쿼리 파라미터(areaCode/sigunguCode/contentTypeId/keyword/page/size/mapX/mapY/radius)를 포함한다.
     * 빈 결과는 캐시하지 않는다(unless).
     */
    @Cacheable(
            value = CacheConfig.CACHE_ATTRACTIONS,
            key = "T(java.util.Objects).toString(#req.areaCode(),'') + ':' + " +
                  "T(java.util.Objects).toString(#req.sigunguCode(),'') + ':' + " +
                  "T(java.util.Objects).toString(#req.contentTypeId(),'') + ':' + " +
                  "T(java.util.Objects).toString(#req.keyword(),'').trim().toLowerCase() + ':' + " +
                  "(#req.page() <= 0 ? 1 : #req.page()) + ':' + #req.clampedSize() + ':' + " +
                  "T(java.util.Objects).toString(#req.mapX(),'') + ':' + " +
                  "T(java.util.Objects).toString(#req.mapY(),'') + ':' + " +
                  "(#req.hasCoords() ? #req.clampedRadius() : 0)",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<AttractionItem> search(AttractionSearchRequestDto req) {
        // keyword 유효성 검사
        String keyword = req.keyword() == null ? "" : req.keyword().trim();
        if (!keyword.isEmpty() && keyword.length() < 2) {
            throw new AttractionHandler(ResponseCode.INVALID_KEYWORD);
        }

        int size = req.clampedSize();
        int page = req.page() <= 0 ? 1 : req.page();

        // stale 폴백용 키 — 파라미터 정규화 후 SHA-256
        String cacheKey = PREFIX_SEARCH + buildSearchHash(keyword, req, size, page);

        // TourAPI 호출
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

            // 빈 결과는 stale 폴백 캐시에 저장하지 않는다(#19) — 빈 응답을 stale로 굳히지 않음.
            // (@Cacheable도 unless로 빈 결과를 캐시하지 않으므로 두 캐시 동작을 일치시킨다.)
            if (items.isEmpty()) {
                log.info("TourAPI 검색 실시간-빈 결과 — stale 캐시 저장 건너뜀. key={}", cacheKey);
                return items;
            }

            // 성공(비어있지 않은) 응답만 stale 폴백 캐시에 백업(외부 장애 시 만료 캐시 반환용)
            stringRedisTemplate.opsForValue().set(cacheKey, serializeItems(items), TTL_SEARCH);
            return items;

        } catch (Exception e) {
            log.warn("TourAPI 검색 실패 — stale 캐시 반환 시도. key={}, error={}", cacheKey, e.getMessage());
            // stale 재시도 (TTL 만료 직전 캐시가 있을 수 있음). 빈 결과는 애초에 저장되지 않으므로
            // stale은 항상 비어있지 않은 과거 응답이다(폴백-빈 vs 실시간-빈 구분 가능).
            String stale = stringRedisTemplate.opsForValue().get(cacheKey);
            if (stale != null) {
                List<AttractionItem> staleItems = deserializeItems(stale);
                log.info("TourAPI 검색 stale 폴백 반환. key={}, size={}", cacheKey, staleItems.size());
                return staleItems;
            }
            throw new AttractionHandler(ResponseCode.EXTERNAL_API_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 상세 조회 (detailCommon2)
    // ─────────────────────────────────────────────────────────────

    /**
     * 상세(detailCommon2) 조회 — Redis("attractionDetail", 30분) 캐시.
     * 키는 contentId. null 결과는 캐시하지 않는다(unless).
     */
    @Cacheable(
            value = CacheConfig.CACHE_ATTRACTION_DETAIL,
            key = "#contentId",
            unless = "#result == null"
    )
    public AttractionItem getDetail(String contentId) {
        String cacheKey = PREFIX_DETAIL + contentId;

        try {
            AttractionItem item = tourApiClient.fetchDetail(contentId);
            if (item == null) {
                throw new AttractionHandler(ResponseCode.ATTRACTION_NOT_FOUND);
            }
            // 성공 응답은 stale 폴백 캐시에도 백업(외부 장애 시 만료 캐시 반환용)
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

    /**
     * 지역코드(areaCode2) 목록 — Redis("attractionAreas", 24시간) 캐시.
     * 인자가 없으므로 단일 키(SimpleKey.EMPTY)로 캐시된다. 빈 결과는 캐시하지 않는다(unless).
     */
    @Cacheable(
            value = CacheConfig.CACHE_ATTRACTION_AREAS,
            unless = "#result == null || #result.isEmpty()"
    )
    public List<AreaItem> getAreas() {
        try {
            List<AreaItem> areas = tourApiClient.fetchAreaCodes(null);
            // 성공 응답은 stale 폴백 캐시에도 백업(외부 장애 시 만료 캐시 반환용)
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
