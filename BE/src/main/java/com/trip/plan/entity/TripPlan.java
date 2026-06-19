package com.trip.plan.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * trip_plans 테이블 엔티티
 * - userId Long 컬럼으로 단순화 (User 엔티티 조인 불필요)
 * - @Version: 낙관적 잠금. plan 하위 모든 변경(place 추가/삭제/교체, day memo)에서 version 증가
 *   → addPlace/removePlace/replacePlaces 에서 entityManager.lock(OPTIMISTIC_FORCE_INCREMENT) 사용
 */
@Entity
@Table(
        name = "trip_plans",
        indexes = {
                @Index(name = "idx_plans_user", columnList = "user_id, updated_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CompanionsType companions;

    private Integer budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OriginType origin;

    /**
     * 공개 공유 토큰. null이면 미공유 상태.
     * 공유 활성화 시 UUID 기반 토큰을 1회 발급하고 이후 재사용한다(idempotent).
     * unique 제약으로 토큰 충돌 방지. (ddl-auto=update가 컬럼을 추가)
     */
    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    /**
     * 낙관적 잠금 버전.
     * plan 메타 수정뿐 아니라 days/places 변경 시에도
     * entityManager.lock(plan, LockModeType.OPTIMISTIC_FORCE_INCREMENT)으로 명시적 증가.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNo ASC")
    private List<TripDay> days = new ArrayList<>();

    @Builder
    public TripPlan(Long userId, String title, LocalDate startDate, LocalDate endDate,
                    CompanionsType companions, Integer budget, OriginType origin) {
        this.userId     = userId;
        this.title      = title;
        this.startDate  = startDate;
        this.endDate    = endDate;
        this.companions = companions;
        this.budget     = budget;
        this.origin     = origin != null ? origin : OriginType.MANUAL;
    }

    /** 메타 수정 */
    public void updateMeta(String title, LocalDate startDate, LocalDate endDate,
                           CompanionsType companions, Integer budget) {
        this.title      = title;
        this.startDate  = startDate;
        this.endDate    = endDate;
        this.companions = companions;
        this.budget     = budget;
    }

    /** 공유 토큰 발급. 이미 발급된 경우 호출 측에서 재사용하므로 여기서는 단순 세팅만. */
    public void markShared(String token) {
        this.shareToken = token;
    }
}
