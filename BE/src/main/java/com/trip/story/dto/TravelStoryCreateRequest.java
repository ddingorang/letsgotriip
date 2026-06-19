package com.trip.story.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 여행 스토리 생성 요청.
 */
public record TravelStoryCreateRequest(
        @NotBlank String title,
        Long planId,
        String beforeNote,
        String afterNote,
        @Min(1) @Max(5) Integer rating,
        String coverImageUrl
) {
}
