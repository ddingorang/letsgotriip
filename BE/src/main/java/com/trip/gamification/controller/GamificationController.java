package com.trip.gamification.controller;

import com.trip.gamification.dto.GamificationSummaryDto;
import com.trip.gamification.service.GamificationService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/gamification/summary — 내 챌린지 진행 + 뱃지 + 통계 (인증 필요)
 */
@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/summary")
    public ResponseEntity<GamificationSummaryDto> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gamificationService.getSummary(principal.userId()));
    }
}
