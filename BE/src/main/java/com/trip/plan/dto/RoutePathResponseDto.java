package com.trip.plan.dto;

import java.util.List;

/**
 * 일자별 자동차 도로 경로 — 카카오 모빌리티 길찾기 기반.
 * path 는 도로를 따라가는 [lat, lng] 좌표열(프론트 polyline 용).
 * enabled=false 면 키 미설정/조회 실패로 경로선 없이 마커만 표시한다.
 */
public record RoutePathResponseDto(
        Long planId,
        boolean enabled,
        List<DayPath> days
) {
    /**
     * partial=true 면 이 일자의 경로가 일부 직선 근사(비도로 구간 폴백)이거나 조회 실패라
     * 실제 도로경로와 다를 수 있음을 뜻한다(프론트에서 '일부 구간 직선 근사' 안내에 사용).
     */
    public record DayPath(
            int dayNo,
            int distanceMeters,
            int durationSeconds,
            int taxiFare,
            int tollFare,
            boolean partial,
            List<double[]> path
    ) {}
}
