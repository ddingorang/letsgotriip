package com.trip.attraction.controller;

import com.trip.attraction.client.AttractionTourApiClient;
import com.trip.attraction.dto.AttractionSearchRequestDto;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.dto.AttractionTourApiResponse.AreaItem;
import com.trip.attraction.dto.AttractionTourApiResponse.ImageItem;
import com.trip.attraction.service.AttractionService;
import com.trip.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * GET /api/attractions       — TourAPI 프록시 검색 (공개, 인증 불요)
 * GET /api/attractions/areas — 지역코드 목록 (/areas는 /{contentId}보다 먼저 선언)
 * GET /api/attractions/{contentId} — 상세 조회
 */
@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService      attractionService;
    private final FestivalService        festivalService;
    private final AttractionTourApiClient tourApiClient;

    /**
     * GET /api/attractions
     * 파라미터: areaCode, sigunguCode, contentTypeId, keyword(≥2자), page, size
     */
    @GetMapping
    public ResponseEntity<List<AttractionItem>> search(
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String mapX,    // 경도(lng) — 내 위치 근처 검색
            @RequestParam(required = false) String mapY,    // 위도(lat)
            @RequestParam(required = false) Integer radius) {  // 반경(m, 기본 5000)

        // contentTypeId=15(축제)는 날짜 윈도우 필터가 있는 Festival 도메인으로 위임
        if ("15".equals(contentTypeId)) {
            return ResponseEntity.ok(festivalService.searchAsItems(keyword, areaCode, mapX, mapY, radius));
        }

        AttractionSearchRequestDto req = new AttractionSearchRequestDto(
                areaCode, sigunguCode, contentTypeId, keyword, page, size, mapX, mapY, radius);
        List<AttractionItem> items = attractionService.search(req);

        // 전체 검색(contentTypeId 미지정)일 때 TourAPI가 반환하는 축제(15) 항목은
        // 날짜 필터가 없으므로 제거 후 Festival 도메인 결과(날짜 윈도우 적용)로 교체.
        // 총 결과 수가 size를 크게 초과하지 않도록 남은 슬롯만큼만 축제 추가.
        if (contentTypeId == null || contentTypeId.isBlank()) {
            List<AttractionItem> nonFestivals = items.stream()
                    .filter(i -> !"15".equals(i.contentTypeId()))
                    .toList();
            int festivalSlots = Math.max(0, size - nonFestivals.size());
            List<AttractionItem> festivals = festivalService
                    .searchAsItems(keyword, areaCode, mapX, mapY, radius)
                    .stream().limit(festivalSlots).toList();
            List<AttractionItem> merged = new ArrayList<>(nonFestivals);
            merged.addAll(festivals);
            return ResponseEntity.ok(merged);
        }

        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/attractions/areas
     * 지역코드 목록 — 24시간 캐시. /{contentId} 매핑보다 먼저 선언해야 함.
     */
    @GetMapping("/areas")
    public ResponseEntity<List<AreaItem>> getAreas() {
        return ResponseEntity.ok(attractionService.getAreas());
    }

    /**
     * GET /api/attractions/{contentId}
     * detailCommon2 — 6시간 캐시
     */
    @GetMapping("/{contentId}")
    public ResponseEntity<AttractionItem> getDetail(@PathVariable String contentId) {
        return ResponseEntity.ok(attractionService.getDetail(contentId));
    }

    /**
     * GET /api/attractions/{contentId}/images
     * detailImage2 — 추가 이미지 목록
     */
    @GetMapping("/{contentId}/images")
    public ResponseEntity<List<ImageItem>> getImages(@PathVariable String contentId) {
        return ResponseEntity.ok(tourApiClient.fetchImages(contentId));
    }
}
