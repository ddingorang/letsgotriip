package com.trip.attraction.controller;

import com.trip.attraction.dto.AttractionSearchRequestDto;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.dto.AttractionTourApiResponse.AreaItem;
import com.trip.attraction.service.AttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private final AttractionService attractionService;

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

        AttractionSearchRequestDto req = new AttractionSearchRequestDto(
                areaCode, sigunguCode, contentTypeId, keyword, page, size, mapX, mapY, radius);
        return ResponseEntity.ok(attractionService.search(req));
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
}
