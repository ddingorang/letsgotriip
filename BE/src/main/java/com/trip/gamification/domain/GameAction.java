package com.trip.gamification.domain;

/**
 * 적립 트리거가 되는 활동 종류와 기본 포인트/EXP.
 * GamificationEventListener 가 NotificationEvent 를 이 enum 으로 매핑해 award 한다.
 */
public enum GameAction {

    /** 내 글/댓글이 좋아요·댓글 등 반응을 받음 */
    COMMUNITY_REACTION_RECEIVED(3, 5),
    /** 동행 신청/수락 등 동행 활동 */
    COMPANION_ACTIVITY(10, 20);

    private final int points;
    private final int exp;

    GameAction(int points, int exp) {
        this.points = points;
        this.exp = exp;
    }

    public int points() {
        return points;
    }

    public int exp() {
        return exp;
    }
}
