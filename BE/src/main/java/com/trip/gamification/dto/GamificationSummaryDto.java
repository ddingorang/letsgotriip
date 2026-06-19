package com.trip.gamification.dto;

import java.util.List;

/**
 * 게임화 요약 — 영속 통계(레벨/포인트/EXP·획득 뱃지·진행 퀘스트)와
 * 기존 데이터(계획·장소 수)에서 파생한 진행 뱃지/챌린지를 합쳐 반환한다.
 */
public record GamificationSummaryDto(
        Level level,
        Stats stats,
        Challenge challenge,
        List<Badge> badges,
        List<QuestDto> quests   // 진행 중(미완료) 퀘스트
) {
    public record Level(
            int level,
            int points,
            int exp,
            int expIntoLevel,   // 현재 레벨 안에서의 EXP (exp % 100)
            int expForNextLevel,// 다음 레벨까지 필요한 총 EXP 구간(=100)
            int percent         // 0~100, 다음 레벨까지의 진행률
    ) {}

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
