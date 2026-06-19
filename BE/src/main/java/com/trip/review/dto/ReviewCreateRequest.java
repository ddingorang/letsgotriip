package com.trip.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** 리뷰 작성 요청. */
public record ReviewCreateRequest(

        @Min(1)
        @Max(5)
        int rating,

        @NotBlank
        String content
) {
}
