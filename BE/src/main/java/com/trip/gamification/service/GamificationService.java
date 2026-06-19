package com.trip.gamification.service;

import com.trip.gamification.dto.GamificationSummaryDto;
import com.trip.gamification.dto.GamificationSummaryDto.Badge;
import com.trip.gamification.dto.GamificationSummaryDto.Challenge;
import com.trip.gamification.dto.GamificationSummaryDto.Stats;
import com.trip.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 게임화(챌린지/뱃지) — 별도 이벤트 추적 없이 기존 계획/장소 수에서 읽기 시점에 파생.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationService {

    private final PlanRepository planRepository;

    private static final int CHALLENGE_GOAL = 10;   // 이번 달 N곳 담기

    public GamificationSummaryDto getSummary(Long userId) {
        long plans = planRepository.countByUserId(userId);
        long places = planRepository.countPlacesByUserId(userId);
        long completed = planRepository.countCompletedByUserId(userId);

        List<Badge> badges = buildBadges(plans, places, completed);
        int unlocked = (int) badges.stream().filter(Badge::unlocked).count();

        Stats stats = new Stats(plans, places, completed, unlocked);
        Challenge challenge = buildChallenge(places);

        return new GamificationSummaryDto(stats, challenge, badges);
    }

    private Challenge buildChallenge(long places) {
        int current = (int) Math.min(places, Integer.MAX_VALUE);
        int percent = Math.min(100, (int) Math.round(current * 100.0 / CHALLENGE_GOAL));
        int remaining = Math.max(0, CHALLENGE_GOAL - current);
        String month = LocalDate.now().getMonthValue() + "월";
        String hint = remaining > 0
                ? remaining + "곳 더 담으면 여행자 뱃지 획득!"
                : "이번 달 목표를 달성했어요! 🎉";
        return new Challenge(
                month + "에 " + CHALLENGE_GOAL + "곳 담기",
                month, current, CHALLENGE_GOAL, percent, remaining, hint);
    }

    private List<Badge> buildBadges(long plans, long places, long completed) {
        List<Badge> list = new ArrayList<>();
        list.add(badge("first_trip", "첫 여행", plans, 1, "star"));
        list.add(badge("planner", "플래너", plans, 3, "calendar"));
        list.add(badge("explorer", "탐험가", places, 5, "location"));
        list.add(badge("spots10", "10곳 달성", places, 10, "map"));
        list.add(badge("veteran", "여행 고수", places, 30, "people"));
        list.add(badge("finisher", "완주자", completed, 1, "check"));
        return list;
    }

    private Badge badge(String key, String name, long value, int goal, String iconType) {
        int current = (int) Math.min(value, goal);
        boolean unlocked = value >= goal;
        String progressText = unlocked ? null : current + "/" + goal;
        return new Badge(key, name, unlocked, current, goal, progressText, iconType);
    }
}
