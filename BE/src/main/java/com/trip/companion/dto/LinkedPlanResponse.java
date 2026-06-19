package com.trip.companion.dto;

import com.trip.plan.dto.PlanDetailResponseDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 동행 게시글에 연결된 여행 계획 요약 + 지도 표시용 장소 목록.
 * 좌표(lat/lng)가 없는 장소는 제외한다.
 */
public record LinkedPlanResponse(
        Long planId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<LinkedPlanPlace> places
) {
    /** 지도 마커용 장소 — 제목·좌표(lat,lng)·일자(dayNo). */
    public record LinkedPlanPlace(
            Integer dayNo,
            String title,
            Double lat,
            Double lng
    ) {}

    /**
     * PlanDetailResponseDto를 동행 연결 요약으로 변환.
     * 각 일자(day)의 장소 중 위/경도가 모두 존재하는 항목만 마커로 포함한다.
     */
    public static LinkedPlanResponse from(PlanDetailResponseDto plan) {
        List<LinkedPlanPlace> places = plan.days().stream()
                .flatMap(day -> day.places().stream()
                        .filter(p -> p.attraction() != null
                                && p.attraction().latitude() != null
                                && p.attraction().longitude() != null)
                        .map(p -> new LinkedPlanPlace(
                                day.dayNo(),
                                p.attraction().title(),
                                p.attraction().latitude(),
                                p.attraction().longitude())))
                .toList();

        return new LinkedPlanResponse(
                plan.id(),
                plan.title(),
                plan.startDate(),
                plan.endDate(),
                places
        );
    }
}
