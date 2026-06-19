package com.trip.review.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관광지 리뷰·평점 — 사용자가 특정 관광지(contentId)에 남기는 별점/후기.
 * 생성/수정 시각은 BaseEntity(createdAt/updatedAt)에서 관리.
 */
@Entity
@Table(
        name = "attraction_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_user_content",
                columnNames = {"user_id", "content_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttractionReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자 식별자 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 관광지 식별자 (관광공사 API contentId) */
    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    /** 별점 1~5 */
    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public AttractionReview(Long userId, String contentId, int rating, String content) {
        this.userId = userId;
        this.contentId = contentId;
        this.rating = rating;
        this.content = content;
    }

    public void update(int rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
