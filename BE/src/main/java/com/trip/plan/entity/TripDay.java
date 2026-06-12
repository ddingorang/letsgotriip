package com.trip.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * trip_days 테이블 엔티티
 * - (plan_id, day_no) 복합 유니크 제약
 * - places는 N+1 방지를 위해 @BatchSize(20) 사용
 *   (컬렉션 2개를 동시에 fetch join하면 MultipleBagFetchException 발생하므로
 *    days는 JPQL fetch join, places는 BatchSize IN-query로 처리)
 */
@Entity
@Table(
        name = "trip_days",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_day", columnNames = {"plan_id", "day_no"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TripPlan plan;

    @Column(name = "day_no", nullable = false)
    private Integer dayNo;

    @Column(length = 300)
    private String memo;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    @BatchSize(size = 20)
    private List<TripPlace> places = new ArrayList<>();

    @Builder
    public TripDay(TripPlan plan, Integer dayNo, String memo) {
        this.plan  = plan;
        this.dayNo = dayNo;
        this.memo  = memo;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
