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
}
