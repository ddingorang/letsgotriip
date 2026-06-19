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
 * signature = "COMMUNITY_REACTION:" + recipientId + ":" + type/title + ":" + link 를
 * 멱등 키로 사용해 동일 트리거에 대한 중복 적립을 막는다.
 * 예: "COMMUNITY_REACTION:42:community/내 글에 좋아요:/community/12"
 *     → 같은 사람이 같은 글에 좋아요 토글을 반복해도 1회만 적립.
 *
 * 유니크 제약은 (user_id, signature) 복합이다. signature 단독 유니크였을 때는
 * 같은 글에 대한 서로 다른 사람의 반응까지 1회로 묶여버려 POPULAR_AUTHOR(반응 5회)가
 * 누적되지 않는 버그가 있었다. signature 에 recipientId 가 포함되므로 사실상 user 별
 * 키이지만, 안전하게 (user_id, signature) 복합 유니크로 둔다.
 */
@Entity
@Table(name = "processed_rewards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_reward_user_signature",
                columnNames = {"user_id", "signature"}))
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
