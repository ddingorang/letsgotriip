package com.trip.gamification.dto;

/**
 * 퀘스트 1건 — 정적 정의(QuestCatalog)와 내 진행(UserQuestProgress)을 합친 표시 모델.
 */
public record QuestDto(
        String code,
        String name,
        String desc,
        int progress,
        int goal,
        int percent,        // 0~100
        boolean completed,
        int rewardExp,
        String iconType,    // map / thumb / people
        String progressText // "3/5 완료"
) {
}
