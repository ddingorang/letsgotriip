// Created: 2026-06-15 23:42:43
package com.trip.companion.dto;

import com.trip.companion.entity.CompanionPost;
import com.trip.companion.entity.enums.CompanionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompanionPostResponse(
        Long id,
        String title,
        LocalDate travelDate,
        String region,
        String duration,
        int maxMembers,
        int estimatedCost,
        String description,
        CompanionStatus status,
        String authorNickname,
        String authorProfileImageUrl,
        Long chatRoomId,
        int currentMembers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanionPostResponse of(CompanionPost post, int currentMembers) {
        return new CompanionPostResponse(
                post.getId(),
                post.getTitle(),
                post.getTravelDate(),
                post.getRegion(),
                post.getDuration(),
                post.getMaxMembers(),
                post.getEstimatedCost(),
                post.getDescription(),
                post.getStatus(),
                post.getAuthor().getNickname(),
                post.getAuthor().getProfileImageUrl(),
                post.getChatRoom() != null ? post.getChatRoom().getId() : null,
                currentMembers,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
