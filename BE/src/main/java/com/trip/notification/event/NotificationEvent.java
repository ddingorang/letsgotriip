package com.trip.notification.event;

/**
 * 알림 생성 도메인 이벤트. 각 도메인 서비스가 행위 발생 시 발행하고,
 * NotificationEventListener가 트랜잭션 커밋 이후 수신해 알림을 적재한다.
 */
public record NotificationEvent(
        Long recipientId,
        String type,     // companion / community / badge / system
        String title,
        String body,
        String link
) {
}
