package com.trip.notification.event;

import com.trip.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 수신 — 원본 도메인 트랜잭션이 커밋된 뒤에만 알림을 적재한다.
 * 알림 생성 실패가 본래 작업(동행 신청·댓글 등)에 영향을 주지 않도록 예외를 흡수.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(NotificationEvent event) {
        try {
            log.info("[알림] 이벤트 수신 — recipient={}, type={}", event.recipientId(), event.type());
            notificationService.create(event);
        } catch (Exception e) {
            log.warn("알림 적재 실패 — recipient={}, type={}, error={}",
                    event.recipientId(), event.type(), e.getMessage());
        }
    }
}
