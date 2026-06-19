package com.trip.review.dto;

import com.trip.review.entity.AttractionReview;

import java.time.LocalDateTime;

/**
 * 리뷰 응답 DTO. 작성자 닉네임은 서비스에서 userId→nickname 매핑 후 주입.
 */
public record ReviewResponse(
        Long id,
        Long userId,
        String nickname,
        String contentId,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(AttractionReview r, String nickname) {
        return new ReviewResponse(
                r.getId(),
                r.getUserId(),
                nickname,
                r.getContentId(),
                r.getRating(),
                r.getContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
