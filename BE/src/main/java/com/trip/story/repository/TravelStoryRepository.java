package com.trip.story.repository;

import com.trip.story.entity.TravelStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelStoryRepository extends JpaRepository<TravelStory, Long> {

    /** 특정 사용자의 스토리 목록 — 최신순 */
    List<TravelStory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 소유 검증용 단건 조회 */
    Optional<TravelStory> findByIdAndUserId(Long id, Long userId);

    /** 여행 계획에 연결된 스토리 */
    Optional<TravelStory> findByPlanId(Long planId);
}
