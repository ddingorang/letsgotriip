package com.trip.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 누적 게임 통계 — 포인트/경험치/레벨을 영속화한다.
 * 레벨은 exp 기반으로 재계산되며(=exp/100+1), 적립은 GamificationService.award 에서만 변경한다.
 */
@Entity
@Table(name = "user_game_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGameStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int exp;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserGameStat(Long userId) {
        this.userId = userId;
        this.points = 0;
        this.exp = 0;
        this.level = 1;
        this.updatedAt = LocalDateTime.now();
    }

    /** 포인트/경험치 누적 후 레벨 재계산. amount 는 양수만 의미가 있다. */
    public void accrue(int pointAmount, int expAmount) {
        this.points += Math.max(0, pointAmount);
        this.exp += Math.max(0, expAmount);
        this.level = this.exp / 100 + 1;
        this.updatedAt = LocalDateTime.now();
    }
}
