package com.trip.gamification.dto;

import java.util.List;

/**
 * 게임화 요약 — 기존 데이터(계획·장소 수)에서 읽기 시점에 파생.
 * 별도 진행 이벤트/엔티티 없음.
 */
public record GamificationSummaryDto(
        Stats stats,
        Challenge challenge,
        List<Badge> badges
) {
    public record Stats(
            long plans,
            long places,
            long completedPlans,
            int badges          // 획득한 뱃지 수
    ) {}

    public record Challenge(
            String title,
            String month,       // "6월"
            int current,
            int goal,
            int percent,        // 0~100
            int remaining,
            String hint
    ) {}

    public record Badge(
            String key,
            String name,
            boolean unlocked,
            int current,
            int goal,
            String progressText, // "7/10" (미획득) / null(획득)
            String iconType      // star / calendar / location / map / check / people
    ) {}
}
