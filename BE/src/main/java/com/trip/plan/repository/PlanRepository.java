package com.trip.plan.repository;

import com.trip.plan.entity.TripPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * TripPlan 레포지토리
 *
 * 상세 조회 fetch 전략 결정:
 * - days와 places를 동시에 fetch join하면 MultipleBagFetchException 발생
 *   (Hibernate는 두 컬렉션 Bag을 동시에 fetch join할 수 없음)
 * - 해결: days만 JPQL fetch join → places는 TripDay에 @BatchSize(20) 적용
 *   → days 1회 IN-query + places N/20회 IN-query (실질적으로 2~3 쿼리)
 *   → EntityGraph(attributePaths={"days"}) 방식 대비 명시적 제어 가능
 */
public interface PlanRepository extends JpaRepository<TripPlan, Long> {

    /**
     * 내 플랜 목록 — updatedAt DESC 정렬은 Pageable sort로 위임
     */
    Page<TripPlan> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    /**
     * 상세 조회 — days fetch join (places는 @BatchSize IN-query로 자동 처리)
     */
    @Query("SELECT DISTINCT p FROM TripPlan p " +
           "LEFT JOIN FETCH p.days d " +
           "WHERE p.id = :id")
    Optional<TripPlan> findByIdWithDays(@Param("id") Long id);

    // ── 게임화(챌린지/뱃지) 집계 ────────────────────────────────
    long countByUserId(Long userId);

    @Query("SELECT COUNT(pl) FROM TripPlan p JOIN p.days d JOIN d.places pl WHERE p.userId = :userId")
    long countPlacesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM TripPlan p WHERE p.userId = :userId AND p.endDate < CURRENT_DATE")
    long countCompletedByUserId(@Param("userId") Long userId);
}
