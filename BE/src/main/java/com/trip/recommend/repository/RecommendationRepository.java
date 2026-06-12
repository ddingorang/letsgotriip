package com.trip.recommend.repository;

import com.trip.recommend.entity.Recommendation;
import com.trip.recommend.entity.RecommendStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Page<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Recommendation> findFirstByUserIdAndRequestHashAndStatusAndCreatedAtAfter(
            Long userId,
            String requestHash,
            RecommendStatus status,
            LocalDateTime createdAtAfter
    );

    /** savePlan 동시성 제어: savedPlanId 재확인을 위한 비관적 쓰기 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recommendation r where r.id = :id")
    Optional<Recommendation> findByIdForUpdate(@Param("id") Long id);
}
