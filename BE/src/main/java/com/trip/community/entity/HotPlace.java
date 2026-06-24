// Created: 2026-06-15 23:24:46
package com.trip.community.entity;

import com.trip.community.entity.enums.HotPlaceCategory;
import com.trip.community.entity.enums.HotPlaceStatus;
import com.trip.user.entity.BaseEntity;
import com.trip.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hot_places")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User submitter;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HotPlaceCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HotPlaceStatus status = HotPlaceStatus.PENDING;

    /** 인기수(좋아요) — '지금 뜨는 여행지' 추천 정렬용. 데모는 시드에서 부여. */
    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    /** 평점(별점, 0~5) — 데모는 시드에서 좋아요수에 비례해 부여. */
    @Column
    private Double rating;

    /** 평점 참여수(리뷰 수) — '(1,248)' 표기용. 데모는 시드에서 부여. */
    @Column(nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    /** 데모 인기수 부여(시드 전용). */
    public void applyDemoLikeCount(int value) {
        this.likeCount = Math.max(0, value);
    }

    /** 데모 별점/리뷰수 부여(시드 전용). */
    public void applyDemoRating(double rating, int ratingCount) {
        this.rating = Math.max(0.0, Math.min(5.0, rating));
        this.ratingCount = Math.max(0, ratingCount);
    }

    public void incLike() { this.likeCount++; }
    public void decLike() { if (this.likeCount > 0) this.likeCount--; }

    public void approve() {
        this.status = HotPlaceStatus.APPROVED;
    }

    public void reject() {
        this.status = HotPlaceStatus.REJECTED;
    }

    public void update(String name, String address, Double latitude, Double longitude,
                       HotPlaceCategory category, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (address != null && !address.isBlank()) this.address = address;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (category != null) this.category = category;
        if (description != null) this.description = description;
    }
}
