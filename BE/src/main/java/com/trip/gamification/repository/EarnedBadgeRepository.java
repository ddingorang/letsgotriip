package com.trip.gamification.repository;

import com.trip.gamification.entity.EarnedBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarnedBadgeRepository extends JpaRepository<EarnedBadge, Long> {

    List<EarnedBadge> findByUserId(Long userId);

    boolean existsByUserIdAndBadgeCode(Long userId, String badgeCode);
}
