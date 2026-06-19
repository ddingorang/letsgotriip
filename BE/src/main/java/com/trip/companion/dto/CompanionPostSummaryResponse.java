// Created: 2026-06-15 23:42:46
package com.trip.companion.dto;

import com.trip.companion.entity.CompanionPost;
import com.trip.companion.entity.enums.CompanionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompanionPostSummaryResponse(
        Long id,
        String title,
        LocalDate travelDate,
        String region,
        String duration,
        int maxMembers,
        int currentMembers,
        CompanionStatus status,
        String authorNickname,
        LocalDateTime createdAt
) {
    public static CompanionPostSummaryResponse of(CompanionPost post, int currentMembers) {
        return new CompanionPostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getTravelDate(),
                post.getRegion(),
                post.getDuration(),
                post.getMaxMembers(),
                currentMembers,
                post.getStatus(),
                post.getAuthor().getNickname(),
                post.getCreatedAt()
        );
    }
}
