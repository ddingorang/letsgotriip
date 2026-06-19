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
 * 이미 적립 처리된 보상 트리거를 기록한다(멱등 키).
 *
 * NotificationEvent 에는 고유 이벤트 ID 가 없으므로, 수신 측(게임화)에서
 * signature = type + ":" + link 를 멱등 키로 사용해 동일 트리거에 대한 중복 적립을 막는다.
 * 예: "COMMUNITY_REACTION:/community/12" → 같은 글에 대한 좋아요 토글 반복 시 1회만 적립.
 */
@Entity
@Table(name = "processed_rewards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_reward_signature",
                columnNames = {"signature"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String signature;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Builder
    public ProcessedReward(Long userId, String signature) {
        this.userId = userId;
        this.signature = signature;
        this.processedAt = LocalDateTime.now();
    }
}
