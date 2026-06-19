package com.trip.gamification.controller;

import com.trip.gamification.dto.GamificationSummaryDto;
import com.trip.gamification.dto.QuestDto;
import com.trip.gamification.service.GamificationService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게임화 API (모두 인증 필요)
 * - GET /api/gamification/summary : 레벨·포인트·EXP + 통계 + 챌린지 + 뱃지 + 진행 중 퀘스트
 * - GET /api/gamification/quests  : 퀘스트 목록 + 내 진행
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

    @GetMapping("/quests")
    public ResponseEntity<List<QuestDto>> getQuests(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gamificationService.getQuests(principal.userId()));
    }
}
