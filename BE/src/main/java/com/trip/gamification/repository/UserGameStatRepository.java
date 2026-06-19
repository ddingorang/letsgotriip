package com.trip.gamification.repository;

import com.trip.gamification.entity.UserGameStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGameStatRepository extends JpaRepository<UserGameStat, Long> {

    Optional<UserGameStat> findByUserId(Long userId);
}
