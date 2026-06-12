package com.trip.preprocessing.repository;

import com.trip.preprocessing.entity.UserAnalysisData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAnalysisDataRepository extends JpaRepository<UserAnalysisData, Long> {
    List<UserAnalysisData> findAllByUserId(Long userId);
}
