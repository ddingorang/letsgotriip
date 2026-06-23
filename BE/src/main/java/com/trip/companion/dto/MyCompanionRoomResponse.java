// Created: 2026-06-18 16:57:40
package com.trip.companion.dto;

import com.trip.companion.entity.CompanionPost;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record MyCompanionRoomResponse(
        Long chatRoomId,
        String title,
        String region,
        LocalDate travelDate,
        Long daysLeft,
        boolean isHost,
        String imageUrl
) {
    public static MyCompanionRoomResponse of(CompanionPost post, boolean isHost) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), post.getTravelDate());
        return new MyCompanionRoomResponse(
                post.getChatRoom().getId(),
                post.getTitle(),
                post.getRegion(),
                post.getTravelDate(),
                days >= 0 ? days : null,
                isHost,
                post.getImageUrl()
        );
    }
}
