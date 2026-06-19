package com.trip.story.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 여행 스토리 부분 수정 요청 — 모든 필드 optional.
 */
public record TravelStoryUpdateRequest(
        String title,
        Long planId,
        String beforeNote,
        String afterNote,
        @Min(1) @Max(5) Integer rating,
        String coverImageUrl
) {
}
