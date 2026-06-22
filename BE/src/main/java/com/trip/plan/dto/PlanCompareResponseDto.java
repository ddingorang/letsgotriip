package com.trip.plan.dto;

import com.trip.plan.entity.TripDay;
import com.trip.plan.entity.TripPlan;
import com.trip.plan.util.RouteCalculator;

import java.util.List;

/**
 * 두 여행 계획 비교 응답(레거시 2개 호출 호환).
 * 각 계획의 요약 통계(PlanStat)를 a/b로 나란히 제공한다.
 * N개 비교는 {@link PlanCompareListResponseDto}를 사용한다.
 */
public record PlanCompareResponseDto(
        PlanStat a,
        PlanStat b
) {
    public static PlanCompareResponseDto of(TripPlan a, TripPlan b) {
        return new PlanCompareResponseDto(PlanStat.from(a), PlanStat.from(b));
    }

    /**
     * 계획별 요약 통계.
     * - totalDistanceKm/totalDurationMin: 기존 동선 로직(RouteCalculator) 재사용으로 산출
     */
    public record PlanStat(
            Long planId,
            String title,
            int totalDays,
            int totalPlaces,
            Integer budget,
            double totalDistanceKm,
            int totalDurationMin
    ) {
        public static PlanStat from(TripPlan plan) {
            int totalDays = plan.getDays().size();
            int totalPlaces = plan.getDays().stream()
                    .mapToInt(d -> d.getPlaces().size())
                    .sum();

            double distance = 0.0;
            int minutes = 0;
            for (TripDay day : plan.getDays()) {
                double dayDist = RouteCalculator.totalDistanceKm(day.getPlaces());
                distance += dayDist;
                minutes += RouteCalculator.estimatedMinutes(dayDist, day.getPlaces().size());
            }

            return new PlanStat(
                    plan.getId(),
                    plan.getTitle(),
                    totalDays,
                    totalPlaces,
                    plan.getBudget(),
                    round1(distance),
                    minutes
            );
        }

        private static double round1(double v) {
            return Math.round(v * 10.0) / 10.0;
        }
    }

    /**
     * N개 계획 비교 응답.
     * 요청한 id 순서 그대로 통계(PlanStat) 리스트를 돌려준다(프론트가 열 순서를 유지하도록).
     */
    public record PlanCompareListResponseDto(
            List<PlanStat> plans
    ) {
        public static PlanCompareListResponseDto of(List<TripPlan> plans) {
            return new PlanCompareListResponseDto(
                    plans.stream().map(PlanStat::from).toList()
            );
        }
    }
}
