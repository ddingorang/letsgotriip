package com.trip.review.dto;

import com.trip.review.entity.AttractionReview;

import java.time.LocalDateTime;

/**
 * 내 리뷰 항목 응답 DTO (G11).
 * attractionName은 서비스에서 contentId→관광지명 매핑 후 주입(미보유 시 null).
 */
public record MyReviewResponse(
        Long id,
        String contentId,
        String attractionName,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MyReviewResponse from(AttractionReview r, String attractionName) {
        return new MyReviewResponse(
                r.getId(),
                r.getContentId(),
                attractionName,
                r.getRating(),
                r.getContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
