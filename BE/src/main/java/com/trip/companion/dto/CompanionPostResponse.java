// Created: 2026-06-15 23:42:43
package com.trip.companion.dto;

import com.trip.companion.entity.CompanionPost;
import com.trip.companion.entity.enums.CompanionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionPostResponse(
        Long id,
        String title,
        LocalDate travelDate,
        String region,
        String duration,
        int maxMembers,
        int estimatedCost,
        String description,
        List<String> tags,
        CompanionStatus status,
        Long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        Long chatRoomId,
        int currentMembers,
        int pendingCount,
        int approvedCount,
        boolean isApplied,       // 현재 로그인 사용자가 신청(PENDING/APPROVED)했는지 여부
        Long myApplicationId,    // 현재 사용자의 활성 신청 ID (신청 취소용). 미신청 시 null
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanionPostResponse of(CompanionPost post, int currentMembers, int pendingCount,
                                           int approvedCount, boolean isApplied, Long myApplicationId) {
        return new CompanionPostResponse(
                post.getId(),
                post.getTitle(),
                post.getTravelDate(),
                post.getRegion(),
                post.getDuration(),
                post.getMaxMembers(),
                post.getEstimatedCost(),
                post.getDescription(),
                post.getTagList(),
                post.getStatus(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getAuthor().getProfileImageUrl(),
                post.getChatRoom() != null ? post.getChatRoom().getId() : null,
                currentMembers,
                pendingCount,
                approvedCount,
                isApplied,
                myApplicationId,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
