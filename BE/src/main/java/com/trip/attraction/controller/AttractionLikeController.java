package com.trip.attraction.controller;

import com.trip.attraction.dto.AttractionLikeResponse;
import com.trip.attraction.dto.CuratedAttractionResponse;
import com.trip.attraction.service.AttractionCuratedService;
import com.trip.attraction.service.AttractionLikeService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 관광지 좋아요(하트) + 큐레이션 조회.
 *  POST /api/attractions/{contentId}/like   — 토글 (인증 필수)
 *  GET  /api/attractions/{contentId}/like   — 상태 조회 (비로그인 허용 → liked=false)
 *  GET  /api/attractions/curated            — 태그/좋아요순 큐레이션 (공개)
 *
 * 좋아요는 찜(Favorite)과 별개의 공개 인기 카운터다.
 * 응답은 프로젝트 관행대로 래퍼 없이 raw DTO 를 ResponseEntity 로 반환한다.
 */
@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class AttractionLikeController {

    private final AttractionLikeService    attractionLikeService;
    private final AttractionCuratedService attractionCuratedService;

    /**
     * GET /api/attractions/curated?tag=food&sort=like&page=0&size=20
     * /{contentId} 매핑보다 먼저 선언될 필요는 없으나(별도 경로) 명확성을 위해 상단 배치.
     */
    @GetMapping("/curated")
    public ResponseEntity<Page<CuratedAttractionResponse>> getCurated(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "like") String sort,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attractionCuratedService.getCurated(tag, sort, page, size));
    }

    /**
     * POST /api/attractions/{contentId}/like?contentType=12&name=...
     * 좋아요 토글 — 인증 필수.
     */
    @PostMapping("/{contentId}/like")
    public ResponseEntity<AttractionLikeResponse> toggleLike(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "12") Integer contentType,
            @RequestParam(required = false) String name,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                attractionLikeService.toggle(principal.userId(), contentId, contentType, name));
    }

    /**
     * GET /api/attractions/{contentId}/like?contentType=12
     * 좋아요 상태 조회 — 비로그인 허용(principal null → liked=false).
     */
    @GetMapping("/{contentId}/like")
    public ResponseEntity<AttractionLikeResponse> getLike(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "12") Integer contentType,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(
                attractionLikeService.getState(userId, contentId, contentType));
    }
}
