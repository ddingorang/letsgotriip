package com.trip.attraction.service;

import com.trip.attraction.client.AttractionTourApiClient;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.entity.Attraction;
import com.trip.attraction.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 관광지 큐레이션 배치 — TourAPI 에서 태그별 관광지를 수집해 스냅샷 upsert + 태그/가짜 좋아요 부여.
 *  food     → areaBasedList2 contentTypeId=39(음식점), arrange=B(조회순)
 *  culture  → areaBasedList2 contentTypeId=14(문화시설)
 *  activity → areaBasedList2 contentTypeId=28(레포츠)
 *  night    → searchKeyword2 "야경" / "전망대"
 * 데모용 가짜 좋아요는 likeCount 가 0인 행에만 부여한다(실제 좋아요 보존).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionBatchService {

    // 다양성을 위해 순회할 지역코드(서울/부산/제주/경기/강원/대구/인천/대전 등)
    private static final String[] AREA_CODES = {"1", "6", "39", "31", "32", "4", "2", "3"};
    private static final int PAGE_SIZE = 40;       // TourAPI 호출당 행 수
    private static final int MAX_PAGES_PER_AREA = 3;

    private final AttractionTourApiClient tourApiClient;
    private final AttractionRepository    attractionRepository;

    /**
     * 배치 실행 — total 을 태그 4종에 ~균등 분배해 수집한다.
     * @return 태그별 upsert 건수 + total
     */
    @Transactional
    public Map<String, Integer> batch(int total) {
        int quotaPerTag = Math.max(1, total / 4);
        Map<String, Integer> counts = new LinkedHashMap<>();

        counts.put("food",     fetchAreaBasedTag("food",     "39", "B", quotaPerTag));
        counts.put("culture",  fetchAreaBasedTag("culture",  "14", "B", quotaPerTag));
        counts.put("activity", fetchAreaBasedTag("activity", "28", "B", quotaPerTag));
        counts.put("night",    fetchNightTag(quotaPerTag));

        int sum = counts.values().stream().mapToInt(Integer::intValue).sum();
        counts.put("total", sum);
        log.info("[AttractionBatch] 배치 완료 — {}", counts);
        return counts;
    }

    /** areaBasedList2 기반 태그 수집(food/culture/activity) */
    private int fetchAreaBasedTag(String tagKey, String contentTypeId, String arrange, int quota) {
        int upserted = 0;
        outer:
        for (String area : AREA_CODES) {
            for (int page = 1; page <= MAX_PAGES_PER_AREA; page++) {
                if (upserted >= quota) break outer;
                List<AttractionItem> items = safeAreaBased(area, contentTypeId, page, arrange);
                if (items.isEmpty()) break; // 이 지역은 더 없음 → 다음 지역
                for (AttractionItem item : items) {
                    if (upserted >= quota) break outer;
                    if (applyItem(item, contentTypeId, tagKey)) upserted++;
                }
            }
        }
        log.info("[AttractionBatch] tag={} 수집 {}건 (quota {})", tagKey, upserted, quota);
        return upserted;
    }

    /** night 태그 — 키워드("야경","전망대") 기반 수집 */
    private int fetchNightTag(int quota) {
        int upserted = 0;
        String[] keywords = {"야경", "전망대"};
        outer:
        for (String keyword : keywords) {
            for (String area : AREA_CODES) {
                for (int page = 1; page <= MAX_PAGES_PER_AREA; page++) {
                    if (upserted >= quota) break outer;
                    List<AttractionItem> items = safeKeyword(keyword, area, page);
                    if (items.isEmpty()) break;
                    for (AttractionItem item : items) {
                        if (upserted >= quota) break outer;
                        // 키워드 검색 결과는 contentTypeId 가 섞여 있으므로 응답값을 그대로 사용(폴백 12)
                        String ctype = item.contentTypeId() != null && !item.contentTypeId().isBlank()
                                ? item.contentTypeId() : "12";
                        if (applyItem(item, ctype, "night")) upserted++;
                    }
                }
            }
        }
        log.info("[AttractionBatch] tag=night 수집 {}건 (quota {})", upserted, quota);
        return upserted;
    }

    /**
     * 한 아이템을 스냅샷 upsert + 태그/가짜 좋아요 부여.
     * @return 유효 처리(좌표·제목 존재) 여부
     */
    private boolean applyItem(AttractionItem item, String contentTypeId, String tagKey) {
        String contentId = item.contentId();
        if (contentId == null || contentId.isBlank() || item.title() == null || item.title().isBlank()) {
            return false;
        }
        Integer contentType = parseInt(contentTypeId);
        if (contentType == null) contentType = 12;

        Double lat = parseDouble(item.mapy()); // 위도
        Double lng = parseDouble(item.mapx()); // 경도

        final Integer ctype = contentType;
        Attraction attraction = attractionRepository
                .findByContentIdAndContentType(contentId, ctype)
                .map(existing -> {
                    // 좌표가 응답에 없으면 기존 값 보존(null 덮어쓰기 방지)
                    Double newLat = lat != null ? lat : existing.getLatitude();
                    Double newLng = lng != null ? lng : existing.getLongitude();
                    existing.update(
                            item.title(), item.addr1(), item.areaCode(), item.sigunguCode(),
                            newLat, newLng, item.firstimage(), item.tel(), existing.getOverview());
                    return existing;
                })
                .orElseGet(() -> {
                    try {
                        return attractionRepository.save(Attraction.builder()
                                .contentId(contentId)
                                .contentType(ctype)
                                .title(item.title())
                                .addr(item.addr1())
                                .areaCode(item.areaCode())
                                .sigunguCode(item.sigunguCode())
                                .latitude(lat)
                                .longitude(lng)
                                .imageUrl(item.firstimage())
                                .tel(item.tel())
                                .fetchedAt(LocalDateTime.now())
                                .likeCount(0)
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        return attractionRepository.findByContentIdAndContentType(contentId, ctype)
                                .orElseThrow(() -> e);
                    }
                });

        // 태그 누적(두 태그에 걸리면 둘 다 보유)
        attraction.addTag(tagKey);
        // 가짜 좋아요 — likeCount 가 0일 때만(실제 좋아요 보존). 일부는 부스트.
        attraction.setLikeCountIfZero(fakeLikeCount());
        return true;
    }

    /** 데모 인기도 — 10~999, 약 10%는 1000~2000으로 부스트 */
    private int fakeLikeCount() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (r.nextInt(100) < 10) {
            return 1000 + r.nextInt(1001); // 부스트
        }
        return 10 + r.nextInt(990);
    }

    // ─────────────────────────────────────────────────────────────
    // TourAPI 호출 — 실패는 빈 리스트로 흡수(한 페이지 실패가 전체를 멈추지 않게)
    // ─────────────────────────────────────────────────────────────
    private List<AttractionItem> safeAreaBased(String area, String contentTypeId, int page, String arrange) {
        try {
            return tourApiClient.fetchAreaBased(area, null, contentTypeId, page, PAGE_SIZE, arrange);
        } catch (Exception e) {
            log.warn("[AttractionBatch] areaBased 실패 area={} ctype={} page={} — skip. err={}",
                    area, contentTypeId, page, e.getMessage());
            return List.of();
        }
    }

    private List<AttractionItem> safeKeyword(String keyword, String area, int page) {
        try {
            return tourApiClient.fetchByKeyword(keyword, area, null, null, page, PAGE_SIZE);
        } catch (Exception e) {
            log.warn("[AttractionBatch] keyword 실패 kw={} area={} page={} — skip. err={}",
                    keyword, area, page, e.getMessage());
            return List.of();
        }
    }

    private Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
