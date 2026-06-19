package com.trip.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** 리뷰 수정 요청. */
public record ReviewUpdateRequest(

        @Min(1)
        @Max(5)
        int rating,

        @NotBlank
        String content
) {
}
