package com.trip.gamification.event;

import com.trip.gamification.domain.GameAction;
import com.trip.gamification.service.GamificationService;
import com.trip.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 기존 NotificationEvent 를 소비해 실제 적립을 수행한다(커밋 이후).
 *
 * 알림 이벤트의 recipientId 는 "혜택을 받는 당사자"이다.
 * - community 좋아요/댓글 → 글/댓글 작성자가 반응을 받음 → COMMUNITY_REACTION_RECEIVED
 * - companion 신청 도착 → 모집글 작성자, 신청 수락 → 신청자 → 둘 다 COMPANION_ACTIVITY
 *
 * 적립 실패가 본래 작업이나 알림 적재에 영향을 주지 않도록 예외를 흡수한다.
 * 새 트랜잭션(REQUIRES_NEW)에서 동작해 커밋된 원본 트랜잭션과 독립적으로 영속화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {

    private final GamificationService gamificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationEvent(NotificationEvent event) {
        try {
            GameAction action = mapAction(event);
            if (action == null || event.recipientId() == null) {
                return;
            }
            gamificationService.award(event.recipientId(), action, 1, dedupSignature(event, action));
        } catch (Exception e) {
            log.warn("게임화 적립 실패 — recipient={}, type={}, error={}",
                    event.recipientId(), event.type(), e.getMessage());
        }
    }

    /**
     * 멱등 키를 만든다.
     *
     * NotificationEvent 에는 고유 이벤트 ID 가 없으므로, 토글로 반복 발행될 수 있는
     * 커뮤니티 반응(좋아요 등)만 dedup 한다. 단, 글ID(link)만으로 키를 만들면
     * "글당 1회"가 되어 같은 글에 여러 사람이 반응해도 1회만 적립되는 버그가 생긴다.
     * → 트리거를 유일하게 식별하도록 recipientId(수신자) + 반응 종류(type/title) + 대상(link)
     *   을 모두 포함한다. 이래야 POPULAR_AUTHOR(반응 5회 받기) 등이 정상 누적된다.
     *
     * 같은 사람이 같은 글에 좋아요를 토글(반복)하는 경우는 동일 시그니처가 되어 1회만 적립되고,
     * 서로 다른 사람의 반응이나 좋아요/댓글 등 다른 종류의 반응은 서로 다른 시그니처가 되어
     * 정상적으로 누적된다.
     * link 가 없으면 트리거를 식별할 수 없으므로 null(=기존 동작, 무조건 적립)로 둔다.
     * 동행 활동은 도착/수락이 서로 다른 정당한 이벤트이므로 dedup 하지 않는다.
     */
    private String dedupSignature(NotificationEvent event, GameAction action) {
        if (action != GameAction.COMMUNITY_REACTION_RECEIVED) {
            return null;
        }
        String link = event.link();
        if (link == null || link.isBlank()) {
            return null;
        }
        // recipient + 반응 종류(type/title) + 대상(link) 조합으로 최대한 유일하게 구성.
        return "COMMUNITY_REACTION:" + event.recipientId()
                + ":" + safe(event.type()) + "/" + safe(event.title())
                + ":" + link;
    }

    /** 시그니처 구성 시 null 을 구분자 공백 없이 안전하게 다룬다. */
    private String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * 알림 type/title 을 적립 활동으로 매핑한다.
     * NotificationEvent 의 type 은 community / companion / badge / system 이며,
     * 구체 활동은 title 로 구분한다(다른 도메인 수정 없이 기존 발행값에 맞춤).
     */
    private GameAction mapAction(NotificationEvent event) {
        String type = event.type();
        if (type == null) {
            return null;
        }
        return switch (type) {
            // 글/댓글 작성자가 좋아요·댓글 반응을 받은 경우만 적립
            case "community" -> GameAction.COMMUNITY_REACTION_RECEIVED;
            // 동행 신청 도착(모집자)·수락(신청자)은 적립, 반려는 제외
            case "companion" -> isCompanionReward(event.title())
                    ? GameAction.COMPANION_ACTIVITY
                    : null;
            default -> null;
        };
    }

    private boolean isCompanionReward(String title) {
        if (title == null) {
            return false;
        }
        // "동행 신청이 도착했어요"(모집자) / "동행 신청이 수락되었어요"(신청자)
        return title.contains("도착") || title.contains("수락");
    }
}
