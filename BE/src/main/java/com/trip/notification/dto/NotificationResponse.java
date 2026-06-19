package com.trip.notification.dto;

import com.trip.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String link,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
