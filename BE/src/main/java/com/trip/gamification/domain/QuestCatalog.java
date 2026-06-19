package com.trip.gamification.domain;

import java.util.List;

/**
 * 퀘스트 정적 정의 — 코드/이름/설명/목표/보상EXP/아이콘.
 * 진행 상태는 UserQuestProgress 에 영속화하고, 여기서는 불변 메타데이터만 보관한다.
 */
public final class QuestCatalog {

    private QuestCatalog() {
    }

    public record QuestDef(
            String code,
            String name,
            String desc,
            int goal,
            int rewardExp,
            String iconType   // map / thumb / people
    ) {}

    /** 동행 활동(신청/수락) 발생 시 진행 */
    public static final QuestDef COMPANION_MANAGER =
            new QuestDef("companion_manager", "동행 매니저", "동행 활동 3회 하기", 3, 300, "people");

    /** 커뮤니티 글/댓글에 좋아요·댓글 등 반응을 받을 때 진행 */
    public static final QuestDef POPULAR_AUTHOR =
            new QuestDef("popular_author", "인기 작성자", "내 글 반응 5회 받기", 5, 200, "thumb");

    private static final List<QuestDef> ALL =
            List.of(COMPANION_MANAGER, POPULAR_AUTHOR);

    public static List<QuestDef> all() {
        return ALL;
    }

    public static QuestDef byCode(String code) {
        return ALL.stream()
                .filter(q -> q.code().equals(code))
                .findFirst()
                .orElse(null);
    }
}
