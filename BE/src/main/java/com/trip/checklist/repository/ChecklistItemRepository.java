package com.trip.checklist.repository;

import com.trip.checklist.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    /** 특정 사용자의 전체 항목 — 정렬순, 동순위면 id순 */
    List<ChecklistItem> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    /** 특정 사용자의 특정 계획 항목 — 정렬순, 동순위면 id순 */
    List<ChecklistItem> findByUserIdAndPlanIdOrderBySortOrderAscIdAsc(Long userId, Long planId);

    /** 소유권 확인용 단건 조회 */
    Optional<ChecklistItem> findByIdAndUserId(Long id, Long userId);
}
