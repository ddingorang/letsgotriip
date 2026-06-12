package com.trip.recommend.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.plan.dto.PlanDetailResponseDto;
import com.trip.recommend.dto.RecommendRequestDto;
import com.trip.recommend.dto.RecommendationResponseDto;
import com.trip.recommend.service.RecommendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * /api/recommendations — 추천 도메인 4개 엔드포인트
 * 전체 소유자 검증은 RecommendService 내부에서 수행
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /** POST /api/recommendations — AI 추천 생성 (동기 최대 30s) */
    @PostMapping
    public ResponseEntity<RecommendationResponseDto> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecommendRequestDto req) {
        RecommendationResponseDto result = recommendService.process(principal.userId(), req);
        return ResponseEntity
                .created(URI.create("/api/recommendations/" + result.id()))
                .body(result);
    }

    /** GET /api/recommendations — 내 추천 이력 */
    @GetMapping
    public ResponseEntity<Page<RecommendationResponseDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(recommendService.getHistory(principal.userId(), page, size));
    }

    /** GET /api/recommendations/{id} — 추천 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponseDto> getOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(recommendService.getOne(principal.userId(), id));
    }

    /** POST /api/recommendations/{id}/save-plan — 초안을 여행 계획으로 저장 (멱등) */
    @PostMapping("/{id}/save-plan")
    public ResponseEntity<PlanDetailResponseDto> savePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        PlanDetailResponseDto result = recommendService.savePlan(principal.userId(), id);
        return ResponseEntity.ok(result);
    }
}
