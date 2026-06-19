package com.trip.notification.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.notification.dto.NotificationResponse;
import com.trip.notification.entity.Notification;
import com.trip.notification.event.NotificationEvent;
import com.trip.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 이벤트로부터 알림 적재.
     * AFTER_COMMIT 리스너에서 호출되므로 새 트랜잭션(REQUIRES_NEW)으로 확실히 커밋한다.
     * 저장된 알림을 반환하면 호출자(리스너)가 트랜잭션 커밋 이후 SSE로 푸시한다.
     *
     * @return 저장된 알림 응답, 수신자가 없으면 {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse create(NotificationEvent e) {
        if (e.recipientId() == null) return null;
        Notification saved = notificationRepository.save(Notification.builder()
                .recipientId(e.recipientId())
                .type(e.type())
                .title(e.title())
                .body(e.body())
                .link(e.link())
                .build());
        return NotificationResponse.from(saved);
    }

    /** 내 알림 목록 (최신 50건) */
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** 안 읽은 알림 수 */
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /** 모두 읽음 처리 */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    /** 단건 읽음 처리 (본인 알림만) */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.NOTIFICATION_NOT_FOUND));
        n.markRead();
    }
}
