package com.trip.plan.util;

import com.trip.plan.entity.TripPlace;

import java.util.ArrayList;
import java.util.List;

/**
 * 동선 계산 유틸 — 외부 라우팅 API 없이 좌표(위/경도) 기반 Haversine 거리만 사용.
 * 거리 추정 + 최근접 이웃(nearest-neighbor) 순서 재배치.
 */
public final class RouteCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    /** 도심 평균 이동 속도(km/h) 가정 */
    private static final double AVG_SPEED_KMH = 30.0;
    /** 장소당 체류 시간(분) 가정 */
    private static final int DWELL_MINUTES = 40;

    private RouteCalculator() {}

    /** 두 좌표 사이 거리(km). 좌표가 없으면 0. */
    public static double haversineKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 인접 장소 간 거리 합(km). */
    public static double totalDistanceKm(List<TripPlace> places) {
        double sum = 0;
        for (int i = 0; i < places.size() - 1; i++) {
            sum += legKm(places.get(i), places.get(i + 1));
        }
        return sum;
    }

    /** 한 구간(두 장소) 거리(km). */
    public static double legKm(TripPlace a, TripPlace b) {
        return haversineKm(
                a.getAttraction().getLatitude(), a.getAttraction().getLongitude(),
                b.getAttraction().getLatitude(), b.getAttraction().getLongitude());
    }

    /** 총 이동거리·체류시간 기반 예상 소요(분). */
    public static int estimatedMinutes(double totalKm, int placeCount) {
        if (placeCount == 0) return 0;
        double travelMin = totalKm / AVG_SPEED_KMH * 60.0;
        return (int) Math.round(travelMin + (double) placeCount * DWELL_MINUTES);
    }

    /**
     * 최근접 이웃 순서 재배치 — 첫 장소를 기준으로 가장 가까운 곳을 차례로 연결.
     * 좌표가 없는 장소는 원래 순서 뒤로 밀린다(거리 0으로 취급되지 않도록 별도 처리 X — MVP).
     * @return 재배치된 placeId 목록
     */
    public static List<Long> nearestNeighborOrder(List<TripPlace> places) {
        if (places.size() <= 2) {
            return places.stream().map(TripPlace::getId).toList();
        }
        List<TripPlace> remaining = new ArrayList<>(places);
        List<Long> order = new ArrayList<>();
        TripPlace current = remaining.remove(0);
        order.add(current.getId());
        while (!remaining.isEmpty()) {
            TripPlace cur = current;
            TripPlace next = remaining.stream()
                    .min((x, y) -> Double.compare(legKm(cur, x), legKm(cur, y)))
                    .orElseThrow();
            remaining.remove(next);
            order.add(next.getId());
            current = next;
        }
        return order;
    }
}
