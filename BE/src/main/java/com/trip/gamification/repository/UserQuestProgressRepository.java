package com.trip.gamification.repository;

import com.trip.gamification.entity.UserQuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, Long> {

    List<UserQuestProgress> findByUserId(Long userId);

    Optional<UserQuestProgress> findByUserIdAndQuestCode(Long userId, String questCode);
}
