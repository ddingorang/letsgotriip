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
    public record DayPath(
            int dayNo,
            int distanceMeters,
            int durationSeconds,
            int taxiFare,
            int tollFare,
            List<double[]> path
    ) {}
}
