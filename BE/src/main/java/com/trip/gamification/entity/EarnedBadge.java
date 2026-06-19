package com.trip.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 영속적으로 획득한 뱃지 — (userId, badgeCode) 유일.
 * 멱등 부여를 위해 유니크 제약을 두고, 중복 INSERT 는 서비스에서 걸러낸다.
 */
@Entity
@Table(name = "earned_badges",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_earned_badge_user_code",
                columnNames = {"userId", "badgeCode"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EarnedBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String badgeCode;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    @Builder
    public EarnedBadge(Long userId, String badgeCode) {
        this.userId = userId;
        this.badgeCode = badgeCode;
        this.earnedAt = LocalDateTime.now();
    }
}
