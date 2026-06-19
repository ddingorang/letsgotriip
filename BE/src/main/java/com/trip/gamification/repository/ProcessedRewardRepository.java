package com.trip.gamification.repository;

import com.trip.gamification.entity.ProcessedReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedRewardRepository extends JpaRepository<ProcessedReward, Long> {

    boolean existsBySignature(String signature);
}
