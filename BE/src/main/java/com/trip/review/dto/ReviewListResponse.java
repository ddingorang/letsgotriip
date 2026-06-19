package com.trip.review.dto;

import java.util.List;

/**
 * 리뷰 목록 + 평균 평점 + 개수 응답 DTO.
 */
public record ReviewListResponse(
        double averageRating,
        long reviewCount,
        List<ReviewResponse> reviews
) {
    public static ReviewListResponse of(double averageRating, List<ReviewResponse> reviews) {
        return new ReviewListResponse(averageRating, reviews.size(), reviews);
    }
}
