package com.trip.plan.dto;

import com.trip.plan.entity.TripDay;
import com.trip.plan.entity.TripPlace;
import com.trip.plan.entity.TripPlan;
import com.trip.plan.util.RouteCalculator;

import java.util.List;

/**
 * 동선 리포트 — 일자별 총 이동거리·예상 소요시간·구간 거리 + 최근접 이웃 추천 순서.
 * 외부 라우팅 API 없이 좌표 기반 Haversine으로 산출.
 */
public record RouteReportResponseDto(
        Long planId,
        double totalDistanceKm,
        int totalEstimatedMinutes,
        List<DayRoute> days
) {
    public static RouteReportResponseDto from(TripPlan plan) {
        List<DayRoute> days = plan.getDays().stream().map(DayRoute::from).toList();
        double total = days.stream().mapToDouble(DayRoute::distanceKm).sum();
        int minutes = days.stream().mapToInt(DayRoute::estimatedMinutes).sum();
        return new RouteReportResponseDto(plan.getId(), round1(total), minutes, days);
    }

    public record DayRoute(
            int dayNo,
            double distanceKm,
            int estimatedMinutes,
            int placeCount,
            List<Leg> legs,
            List<Long> currentOrder,    // 현재 placeId 순서
            List<Long> suggestedOrder,  // 최근접 이웃 추천 순서
            boolean reorderSuggested    // 추천 순서가 현재와 다른지
    ) {
        public static DayRoute from(TripDay day) {
            List<TripPlace> places = day.getPlaces();
            List<Leg> legs = new java.util.ArrayList<>();
            for (int i = 0; i < places.size() - 1; i++) {
                TripPlace a = places.get(i);
                TripPlace b = places.get(i + 1);
                legs.add(new Leg(
                        a.getAttraction().getTitle(),
                        b.getAttraction().getTitle(),
                        round1(RouteCalculator.legKm(a, b))));
            }
            double dist = RouteCalculator.totalDistanceKm(places);
            int minutes = RouteCalculator.estimatedMinutes(dist, places.size());
            List<Long> current = places.stream().map(TripPlace::getId).toList();
            List<Long> suggested = RouteCalculator.nearestNeighborOrder(places);
            return new DayRoute(
                    day.getDayNo(), round1(dist), minutes, places.size(),
                    legs, current, suggested, !current.equals(suggested));
        }
    }

    public record Leg(String from, String to, double distanceKm) {}

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
