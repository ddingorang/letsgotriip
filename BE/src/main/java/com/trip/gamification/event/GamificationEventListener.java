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
            gamificationService.award(event.recipientId(), action, 1);
        } catch (Exception e) {
            log.warn("게임화 적립 실패 — recipient={}, type={}, error={}",
                    event.recipientId(), event.type(), e.getMessage());
        }
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
