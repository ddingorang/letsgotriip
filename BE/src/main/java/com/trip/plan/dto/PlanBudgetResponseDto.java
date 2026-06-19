package com.trip.plan.dto;

import java.util.List;

/**
 * 예산 추정 응답 (카테고리 기반 데모 추정치).
 * 장소에 가격 필드가 없으므로 contentType(TourAPI 유형)별 기본 단가를 합산한다.
 * - dayBudgets: 일자별 추정 비용
 * - totalEstimated: 전체 추정 합계
 * - plannedBudget: 계획에 저장된 예산(있으면)
 * - difference: plannedBudget - totalEstimated (저장 예산 없으면 null)
 */
public record PlanBudgetResponseDto(
        Long planId,
        List<DayBudget> dayBudgets,
        long totalEstimated,
        Integer plannedBudget,
        Integer difference,
        String note
) {
    public record DayBudget(
            int dayNo,
            long estimatedCost
    ) {}
}
