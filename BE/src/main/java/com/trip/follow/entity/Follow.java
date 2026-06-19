package com.trip.follow.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팔로우 — 한 사용자(follower)가 다른 사용자(followee)를 구독.
 * (follower, followee) 조합은 유일(unique)하여 중복 팔로우를 방지한다.
 * 생성 시각은 BaseEntity(createdAt)에서 관리.
 */
@Entity
@Table(
        name = "follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_follow_follower_followee",
                columnNames = {"follower_id", "followee_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 팔로우를 거는 사용자 (구독자) */
    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    /** 팔로우 당하는 사용자 (대상) */
    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    @Builder
    public Follow(Long followerId, Long followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }
}
