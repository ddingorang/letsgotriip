package com.trip.recommend.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * recommendations 테이블 엔티티 — §3 DDL 기준
 */
@Entity
@Table(
        name = "recommendations",
        indexes = {
                @Index(name = "idx_reco_user", columnList = "user_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_json", columnDefinition = "json", nullable = false)
    private String requestJson;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "result_json", columnDefinition = "json")
    private String resultJson;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private RecommendStatus status;

    @Column(name = "saved_plan_id")
    private Long savedPlanId;

    @Column(name = "error_code", length = 30)
    private String errorCode;

    @Column(name = "error_message", length = 300)
    private String errorMessage;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Builder
    public Recommendation(Long userId, String requestJson, String requestHash,
                          String resultJson, String model, RecommendStatus status,
                          Long savedPlanId, String errorCode, String errorMessage,
                          Integer latencyMs) {
        this.userId       = userId;
        this.requestJson  = requestJson;
        this.requestHash  = requestHash;
        this.resultJson   = resultJson;
        this.model        = model;
        this.status       = status;
        this.savedPlanId  = savedPlanId;
        this.errorCode    = errorCode;
        this.errorMessage = errorMessage;
        this.latencyMs    = latencyMs;
    }

    public void markSavedPlan(Long planId) {
        this.savedPlanId = planId;
    }
}
