package com.trip.plan.entity;

import com.trip.attraction.entity.Attraction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * trip_places 테이블 엔티티
 * - (day_id, seq) 유니크: 순서 중복 방지
 * - (day_id, attraction_id) 유니크: 같은 일자 내 같은 장소 중복 방지 (409 DUPLICATE_PLACE)
 */
@Entity
@Table(
        name = "trip_places",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_place_seq",  columnNames = {"day_id", "seq"}),
                @UniqueConstraint(name = "uk_place_attr", columnNames = {"day_id", "attraction_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private TripDay day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;

    @Column(nullable = false)
    private Integer seq;

    @Column(name = "visit_time")
    private LocalTime visitTime;

    @Column(length = 300)
    private String memo;

    @Builder
    public TripPlace(TripDay day, Attraction attraction, Integer seq,
                     LocalTime visitTime, String memo) {
        this.day        = day;
        this.attraction = attraction;
        this.seq        = seq;
        this.visitTime  = visitTime;
        this.memo       = memo;
    }

    public void update(Integer seq, LocalTime visitTime, String memo) {
        this.seq       = seq;
        this.visitTime = visitTime;
        this.memo      = memo;
    }
}
