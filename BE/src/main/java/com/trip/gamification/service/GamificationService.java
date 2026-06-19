package com.trip.gamification.service;

import com.trip.gamification.domain.BadgeCatalog;
import com.trip.gamification.domain.BadgeCatalog.BadgeDef;
import com.trip.gamification.domain.GameAction;
import com.trip.gamification.domain.QuestCatalog;
import com.trip.gamification.domain.QuestCatalog.QuestDef;
import com.trip.gamification.dto.GamificationSummaryDto;
import com.trip.gamification.dto.GamificationSummaryDto.Badge;
import com.trip.gamification.dto.GamificationSummaryDto.Challenge;
import com.trip.gamification.dto.GamificationSummaryDto.Level;
import com.trip.gamification.dto.GamificationSummaryDto.Stats;
import com.trip.gamification.dto.QuestDto;
import com.trip.gamification.entity.EarnedBadge;
import com.trip.gamification.entity.ProcessedReward;
import com.trip.gamification.entity.UserGameStat;
import com.trip.gamification.entity.UserQuestProgress;
import com.trip.gamification.repository.EarnedBadgeRepository;
import com.trip.gamification.repository.ProcessedRewardRepository;
import com.trip.gamification.repository.UserGameStatRepository;
import com.trip.gamification.repository.UserQuestProgressRepository;
import com.trip.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게임화 핵심 서비스.
 *
 * - award(userId, action, amount): 포인트/EXP 누적 후 레벨 재계산 + 관련 퀘스트 진행 + 활동 뱃지 멱등 부여.
 * - getSummary / getQuests: 영속 통계와 기존 계획/장소 파생 데이터를 합쳐 조회.
 *
 * 다른 도메인 서비스는 수정하지 않으며, 기존 NotificationEvent 만 소비한다(리스너 경유).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationService {

    private final PlanRepository planRepository;
    private final UserGameStatRepository statRepository;
    private final EarnedBadgeRepository badgeRepository;
    private final UserQuestProgressRepository questRepository;
    private final ProcessedRewardRepository processedRewardRepository;

    private static final int CHALLENGE_GOAL = 10;   // 이번 달 N곳 담기
    private static final int EXP_PER_LEVEL = 100;

    // ── 적립 ────────────────────────────────────────────────

    /**
     * 활동에 대해 포인트/EXP 를 적립하고 레벨을 재계산한다.
     * amount 는 활동 횟수(가중치)로, 포인트/EXP/퀘스트 진행에 동일 배수로 적용된다.
     */
    @Transactional
    public void award(Long userId, GameAction action, int amount) {
        award(userId, action, amount, null);
    }

    /**
     * 멱등 키(dedupSignature)와 함께 적립한다.
     *
     * dedupSignature 가 있으면 ProcessedReward 에 먼저 기록을 시도하고,
     * 이미 처리된 트리거(예: 좋아요 토글 반복)면 적립 없이 종료한다.
     * dedupSignature 가 null 이면 기존 동작과 동일하게 무조건 적립한다.
     */
    @Transactional
    public void award(Long userId, GameAction action, int amount, String dedupSignature) {
        if (userId == null || action == null || amount <= 0) {
            return;
        }

        if (dedupSignature != null && !markProcessed(userId, dedupSignature)) {
            log.debug("[게임화] 중복 트리거 무시 — userId={}, signature={}", userId, dedupSignature);
            return;
        }

        accruePoints(userId, action.points() * amount, action.exp() * amount);

        advanceQuestsFor(userId, action, amount);
        grantBadgesFor(userId, action);

        log.debug("[게임화] 적립 — userId={}, action={}, amount={}, signature={}",
                userId, action, amount, dedupSignature);
    }

    /**
     * 멱등 키를 기록한다. 신규 기록이면 true, 이미 처리된 트리거면 false.
     * 경합으로 유니크 제약 위반이 나도 "이미 처리됨"으로 흡수한다.
     */
    private boolean markProcessed(Long userId, String signature) {
        if (processedRewardRepository.existsBySignature(signature)) {
            return false;
        }
        try {
            processedRewardRepository.saveAndFlush(
                    ProcessedReward.builder().userId(userId).signature(signature).build());
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    /**
     * 포인트/EXP 를 원자적 UPDATE 로 적립한다(lost update 방지).
     * 대상 행이 아직 없으면 생성 후 1회 재시도한다.
     */
    private void accruePoints(Long userId, int points, int exp) {
        int updated = statRepository.accrue(userId, Math.max(0, points), Math.max(0, exp), LocalDateTime.now());
        if (updated == 0) {
            createStat(userId);
            statRepository.accrue(userId, Math.max(0, points), Math.max(0, exp), LocalDateTime.now());
        }
    }

    private UserGameStat createStat(Long userId) {
        try {
            return statRepository.saveAndFlush(UserGameStat.builder().userId(userId).build());
        } catch (DataIntegrityViolationException e) {
            // 동시 생성 경합 — 이미 만들어진 행을 재조회
            return statRepository.findByUserId(userId)
                    .orElseThrow(() -> e);
        }
    }

    private void advanceQuestsFor(Long userId, GameAction action, int amount) {
        switch (action) {
            case COMPANION_ACTIVITY -> advanceQuest(userId, QuestCatalog.COMPANION_MANAGER, amount);
            case COMMUNITY_REACTION_RECEIVED -> advanceQuest(userId, QuestCatalog.POPULAR_AUTHOR, amount);
        }
    }

    /**
     * 퀘스트 진행을 times 만큼 전진시킨다.
     * 진행 행을 먼저 보장(없으면 생성)한 뒤, 각 단계를 원자적 UPDATE 로 증가시켜
     * 동시 적립 간 lost update 를 방지한다. 이미 완료된 퀘스트면 증가 없이 멱등 종료한다.
     */
    private void advanceQuest(Long userId, QuestDef quest, int times) {
        ensureQuest(userId, quest.code());
        for (int i = 0; i < times; i++) {
            questRepository.incrementProgress(userId, quest.code(), quest.goal(), LocalDateTime.now());
        }
    }

    /** 진행 행이 없으면 생성한다(경합 시 유니크 제약으로 흡수). */
    private void ensureQuest(Long userId, String questCode) {
        if (questRepository.findByUserIdAndQuestCode(userId, questCode).isPresent()) {
            return;
        }
        try {
            questRepository.saveAndFlush(
                    UserQuestProgress.builder().userId(userId).questCode(questCode).build());
        } catch (DataIntegrityViolationException e) {
            log.debug("[게임화] 퀘스트 진행 행 동시 생성 무시 — userId={}, quest={}", userId, questCode);
        }
    }

    private void grantBadgesFor(Long userId, GameAction action) {
        switch (action) {
            case COMPANION_ACTIVITY -> grantBadge(userId, BadgeCatalog.FIRST_COMPANION.code());
            case COMMUNITY_REACTION_RECEIVED -> grantBadge(userId, BadgeCatalog.FIRST_CHEER.code());
        }
    }

    /** 뱃지를 멱등 부여한다. 이미 있으면 무시(경합 시 유니크 제약으로 흡수). */
    private void grantBadge(Long userId, String badgeCode) {
        if (badgeRepository.existsByUserIdAndBadgeCode(userId, badgeCode)) {
            return;
        }
        try {
            badgeRepository.save(EarnedBadge.builder().userId(userId).badgeCode(badgeCode).build());
        } catch (DataIntegrityViolationException e) {
            log.debug("[게임화] 뱃지 중복 부여 무시 — userId={}, badge={}", userId, badgeCode);
        }
    }

    // ── 조회: 요약 ───────────────────────────────────────────

    public GamificationSummaryDto getSummary(Long userId) {
        long plans = planRepository.countByUserId(userId);
        long places = planRepository.countPlacesByUserId(userId);
        long completed = planRepository.countCompletedByUserId(userId);

        UserGameStat stat = statRepository.findByUserId(userId).orElse(null);
        Level level = buildLevel(stat);

        Set<String> earned = badgeRepository.findByUserId(userId).stream()
                .map(EarnedBadge::getBadgeCode)
                .collect(Collectors.toSet());

        List<Badge> badges = buildBadges(plans, places, completed, earned);
        int unlocked = (int) badges.stream().filter(Badge::unlocked).count();

        Stats stats = new Stats(plans, places, completed, unlocked);
        Challenge challenge = buildChallenge(places);
        List<QuestDto> inProgress = buildQuests(userId).stream()
                .filter(q -> !q.completed())
                .toList();

        return new GamificationSummaryDto(level, stats, challenge, badges, inProgress);
    }

    private Level buildLevel(UserGameStat stat) {
        int points = stat == null ? 0 : stat.getPoints();
        int exp = stat == null ? 0 : stat.getExp();
        int lvl = stat == null ? 1 : stat.getLevel();
        int into = exp % EXP_PER_LEVEL;
        int percent = (int) Math.round(into * 100.0 / EXP_PER_LEVEL);
        return new Level(lvl, points, exp, into, EXP_PER_LEVEL, percent);
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

    /**
     * 진행 뱃지(계획/장소 파생) + 영속 활동 뱃지를 합쳐 표시 목록을 만든다.
     * 영속 뱃지는 EarnedBadge 존재 여부로 unlocked 를 결정한다.
     */
    private List<Badge> buildBadges(long plans, long places, long completed, Set<String> earned) {
        List<Badge> list = new ArrayList<>();
        // 계획/장소 파생 진행 뱃지
        list.add(progressBadge("first_trip", "첫 여행", plans, 1, "star"));
        list.add(progressBadge("planner", "플래너", plans, 3, "calendar"));
        list.add(progressBadge("explorer", "탐험가", places, 5, "location"));
        list.add(progressBadge("spots10", "10곳 달성", places, 10, "map"));
        list.add(progressBadge("veteran", "여행 고수", places, 30, "people"));
        list.add(progressBadge("finisher", "완주자", completed, 1, "check"));
        // 영속(이벤트 기반) 활동 뱃지
        for (BadgeDef def : BadgeCatalog.persistent()) {
            list.add(earnedBadge(def, earned.contains(def.code())));
        }
        return list;
    }

    private Badge progressBadge(String key, String name, long value, int goal, String iconType) {
        int current = (int) Math.min(value, goal);
        boolean unlocked = value >= goal;
        String progressText = unlocked ? null : current + "/" + goal;
        return new Badge(key, name, unlocked, current, goal, progressText, iconType);
    }

    private Badge earnedBadge(BadgeDef def, boolean unlocked) {
        int current = unlocked ? 1 : 0;
        String progressText = unlocked ? null : "0/1";
        return new Badge(def.code(), def.name(), unlocked, current, 1, progressText, def.iconType());
    }

    // ── 조회: 퀘스트 ─────────────────────────────────────────

    /** 퀘스트 전체(정의 + 내 진행). 미진행 퀘스트는 progress=0 으로 노출. */
    public List<QuestDto> getQuests(Long userId) {
        return buildQuests(userId);
    }

    private List<QuestDto> buildQuests(Long userId) {
        List<UserQuestProgress> mine = questRepository.findByUserId(userId);
        List<QuestDto> result = new ArrayList<>();
        for (QuestDef def : QuestCatalog.all()) {
            UserQuestProgress p = mine.stream()
                    .filter(q -> q.getQuestCode().equals(def.code()))
                    .findFirst()
                    .orElse(null);
            int progress = p == null ? 0 : Math.min(p.getProgress(), def.goal());
            boolean completed = p != null && p.isCompleted();
            int percent = Math.min(100, (int) Math.round(progress * 100.0 / def.goal()));
            String progressText = progress + "/" + def.goal() + " 완료";
            result.add(new QuestDto(
                    def.code(), def.name(), def.desc(),
                    progress, def.goal(), percent, completed,
                    def.rewardExp(), def.iconType(), progressText));
        }
        return result;
    }
}
