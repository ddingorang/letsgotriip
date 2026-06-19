package com.trip.gamification.domain;

import java.util.List;

/**
 * 뱃지 정적 정의.
 *
 * 두 갈래로 나뉜다.
 * - 영속 뱃지(persistent): 이벤트로 멱등 부여되어 EarnedBadge 에 기록된다(첫 글·첫 동행 등).
 * - 진행 뱃지(progress): 기존 계획/장소 수에서 읽기 시점에 파생된다(요약에만 노출).
 *
 * 프런트(BadgesView)는 key/name/unlocked/iconType 만 사용하므로 그 형태를 유지한다.
 */
public final class BadgeCatalog {

    private BadgeCatalog() {
    }

    public record BadgeDef(
            String code,
            String name,
            String iconType   // star / calendar / location / map / check / people
    ) {}

    // ── 영속(이벤트 기반) 뱃지 ───────────────────────────────
    /** 첫 동행 신청/수락 등 동행 활동 1회 */
    public static final BadgeDef FIRST_COMPANION =
            new BadgeDef("first_companion", "첫 동행", "people");
    /** 내 글/댓글이 처음으로 반응(좋아요/댓글)을 받음 */
    public static final BadgeDef FIRST_CHEER =
            new BadgeDef("first_cheer", "첫 응원", "check");

    private static final List<BadgeDef> PERSISTENT =
            List.of(FIRST_COMPANION, FIRST_CHEER);

    public static List<BadgeDef> persistent() {
        return PERSISTENT;
    }

    public static BadgeDef byCode(String code) {
        return PERSISTENT.stream()
                .filter(b -> b.code().equals(code))
                .findFirst()
                .orElse(null);
    }
}
