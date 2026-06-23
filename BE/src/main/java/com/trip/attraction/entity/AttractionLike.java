package com.trip.attraction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * attraction_like 테이블 — 사용자별 좋아요(하트) 상태.
 * (userId, contentId, contentType) 유니크: 한 사용자가 한 관광지에 1회만 좋아요.
 * 찜(Favorite)과 별개이며, Attraction.likeCount(공개 인기 카운터)를 토글로 증감시킨다.
 */
@Entity
@Table(
        name = "attraction_like",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attr_like_user_content",
                        columnNames = {"user_id", "content_id", "content_type"})
        },
        indexes = {
                @Index(name = "idx_attr_like_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttractionLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", length = 20, nullable = false)
    private String contentId;

    @Column(name = "content_type", nullable = false)
    private Integer contentType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AttractionLike(Long userId, String contentId, Integer contentType, LocalDateTime createdAt) {
        this.userId      = userId;
        this.contentId   = contentId;
        this.contentType = contentType;
        this.createdAt   = createdAt;
    }
}
