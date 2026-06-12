package com.trip.recommend.dto;

import java.util.List;

/**
 * Spring AI BeanOutputConverter 대상 record.
 * LLM이 이 구조로 JSON을 반환한다.
 */
public record ItineraryDraft(
        List<DayPlan> days,
        String totalSummary
) {

    public record DayPlan(
            int dayNo,
            List<PlaceRecommendation> places,
            String summary
    ) {}

    public record PlaceRecommendation(
            String contentId,
            String title,
            String visitTime,  // "HH:mm" — 실패 시 null
            String reason
    ) {}
}
