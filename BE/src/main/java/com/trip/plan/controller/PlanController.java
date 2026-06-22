package com.trip.plan.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.plan.dto.*;
import com.trip.plan.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * /api/plans — plan 도메인 8개 엔드포인트
 * 전체 소유자 검증은 PlanService 내부에서 수행
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /** POST /api/plans — 여행 계획 생성 */
    @PostMapping
    public ResponseEntity<PlanDetailResponseDto> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlanCreateRequestDto req) {
        PlanDetailResponseDto result = planService.create(principal.userId(), req);
        return ResponseEntity
                .created(URI.create("/api/plans/" + result.id()))
                .body(result);
    }

    /** GET /api/plans — 내 여행 계획 목록 */
    @GetMapping
    public ResponseEntity<Page<PlanSummaryResponseDto>> getMyPlans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(planService.getMyPlans(principal.userId(), page, size));
    }

    /** GET /api/plans/{planId} — 여행 계획 상세 */
    @GetMapping("/{planId}")
    public ResponseEntity<PlanDetailResponseDto> getDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ResponseEntity.ok(planService.getDetail(principal.userId(), planId));
    }

    /** GET /api/plans/{planId}/route-report — 동선 리포트(거리·소요시간·추천순서) */
    @GetMapping("/{planId}/route-report")
    public ResponseEntity<com.trip.plan.dto.RouteReportResponseDto> getRouteReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ResponseEntity.ok(planService.getRouteReport(principal.userId(), planId));
    }

    /** GET /api/plans/{planId}/route-path — 일자별 자동차 도로 경로(카카오 길찾기) */
    @GetMapping("/{planId}/route-path")
    public ResponseEntity<com.trip.plan.dto.RoutePathResponseDto> getRoutePath(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ResponseEntity.ok(planService.getRoutePath(principal.userId(), planId));
    }

    /** PATCH /api/plans/{planId} — 여행 계획 메타 수정 */
    @PatchMapping("/{planId}")
    public ResponseEntity<PlanDetailResponseDto> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody PlanUpdateRequestDto req) {
        return ResponseEntity.ok(planService.update(principal.userId(), planId, req));
    }

    /** DELETE /api/plans/{planId} — 여행 계획 삭제 */
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        planService.delete(principal.userId(), planId);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/plans/{planId}/days/{dayNo}/places — 장소 추가 */
    @PostMapping("/{planId}/days/{dayNo}/places")
    public ResponseEntity<PlanDetailResponseDto> addPlace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @PathVariable int dayNo,
            @Valid @RequestBody PlaceAddRequestDto req) {
        return ResponseEntity.ok(planService.addPlace(principal.userId(), planId, dayNo, req));
    }

    /** PUT /api/plans/{planId}/days/{dayNo}/places — 장소 전체 교체 */
    @PutMapping("/{planId}/days/{dayNo}/places")
    public ResponseEntity<PlanDetailResponseDto> replacePlaces(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @PathVariable int dayNo,
            @Valid @RequestBody PlacesReplaceRequestDto req) {
        return ResponseEntity.ok(planService.replacePlaces(principal.userId(), planId, dayNo, req));
    }

    /** DELETE /api/plans/{planId}/days/{dayNo}/places/{placeId} — 장소 삭제 */
    @DeleteMapping("/{planId}/days/{dayNo}/places/{placeId}")
    public ResponseEntity<PlanDetailResponseDto> removePlace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @PathVariable int dayNo,
            @PathVariable Long placeId) {
        return ResponseEntity.ok(planService.removePlace(principal.userId(), planId, dayNo, placeId));
    }

    /** POST /api/plans/{planId}/share — 공유 토큰 발급(소유자) */
    @PostMapping("/{planId}/share")
    public ResponseEntity<PlanShareResponseDto> createShare(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ResponseEntity.ok(planService.createShare(principal.userId(), planId));
    }

    /** GET /api/plans/shared/{token} — 공유 토큰으로 공개 조회(인증 불요) */
    @GetMapping("/shared/{token}")
    public ResponseEntity<PlanDetailResponseDto> getShared(@PathVariable String token) {
        return ResponseEntity.ok(planService.getShared(token));
    }

    /** GET /api/plans/compare?aId&bId — 두 계획 비교(소유자, 레거시 2개 호환) */
    @GetMapping("/compare")
    public ResponseEntity<PlanCompareResponseDto> compare(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long aId,
            @RequestParam Long bId) {
        return ResponseEntity.ok(planService.compare(principal.userId(), aId, bId));
    }

    /** GET /api/plans/compare-many?ids=1,2,3 — N개 계획 비교(모두 소유자 검증) */
    @GetMapping("/compare-many")
    public ResponseEntity<PlanCompareResponseDto.PlanCompareListResponseDto> compareMany(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("ids") java.util.List<Long> ids) {
        return ResponseEntity.ok(planService.compareMany(principal.userId(), ids));
    }

    /** GET /api/plans/{planId}/budget — 카테고리 기반 예산 추정(소유자) */
    @GetMapping("/{planId}/budget")
    public ResponseEntity<PlanBudgetResponseDto> getBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ResponseEntity.ok(planService.getBudget(principal.userId(), planId));
    }
}
