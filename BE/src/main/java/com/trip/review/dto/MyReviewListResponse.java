package com.trip.review.dto;

import java.util.List;

/**
 * 내 리뷰 목록 + 개수 응답 DTO (G11).
 */
public record MyReviewListResponse(
        long reviewCount,
        List<MyReviewResponse> reviews
) {
    public static MyReviewListResponse of(List<MyReviewResponse> reviews) {
        return new MyReviewListResponse(reviews.size(), reviews);
    }
}
