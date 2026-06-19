package com.trip.notification.event;

import com.trip.notification.dto.NotificationResponse;
import com.trip.notification.service.NotificationService;
import com.trip.notification.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 수신 — 원본 도메인 트랜잭션이 커밋된 뒤에만 알림을 적재한다.
 * 알림 생성 실패가 본래 작업(동행 신청·댓글 등)에 영향을 주지 않도록 예외를 흡수.
 * 적재(REQUIRES_NEW 커밋) 직후 SSE로 수신자에게 실시간 푸시한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(NotificationEvent event) {
        NotificationResponse saved;
        try {
            log.info("[알림] 이벤트 수신 — recipient={}, type={}", event.recipientId(), event.type());
            // REQUIRES_NEW 트랜잭션이라 반환 시점에 영속(커밋) 완료
            saved = notificationService.create(event);
        } catch (Exception e) {
            log.warn("알림 적재 실패 — recipient={}, type={}, error={}",
                    event.recipientId(), event.type(), e.getMessage());
            return;
        }
        if (saved == null) return;
        // 적재 성공 이후 푸시 — 전송 실패가 적재에 영향 주지 않도록 분리
        try {
            sseEmitterRegistry.push(event.recipientId(), saved);
        } catch (Exception e) {
            log.warn("알림 SSE 푸시 실패 — recipient={}, error={}", event.recipientId(), e.getMessage());
        }
    }
}
