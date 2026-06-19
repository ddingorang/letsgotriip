package com.trip.story.dto;

import com.trip.story.entity.TravelStory;

import java.time.LocalDateTime;

/**
 * 여행 스토리 응답 DTO. 목록·상세 공용.
 */
public record TravelStoryResponse(
        Long id,
        Long userId,
        Long planId,
        String title,
        String beforeNote,
        String afterNote,
        Integer rating,
        String coverImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TravelStoryResponse from(TravelStory s) {
        return new TravelStoryResponse(
                s.getId(),
                s.getUserId(),
                s.getPlanId(),
                s.getTitle(),
                s.getBeforeNote(),
                s.getAfterNote(),
                s.getRating(),
                s.getCoverImageUrl(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
