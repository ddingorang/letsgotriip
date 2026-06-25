package com.trip.plan.dto;

import java.util.List;

/**
 * 예산 추정 응답.
 * - dayBudgets: 일자별 장소 비용 합계 (인원 반영, 주유비 미포함)
 * - totalEstimated: 장소 비용 + 주유비(경로 확보 시)
 * - fuelCost: 주유비 추정치 (카카오 실도로 경로 있을 때만 non-null)
 * - difference: plannedBudget - totalEstimated (저장 예산 없으면 null)
 */
public record PlanBudgetResponseDto(
        Long planId,
        List<DayBudget> dayBudgets,
        long totalEstimated,
        Integer plannedBudget,
        Integer difference,
        Long fuelCost,
        String note
) {
    public record DayBudget(
            int dayNo,
            long estimatedCost
    ) {}
}
