package com.trip.context.dto;

import java.util.List;

/**
 * 여행 맥락 정보 — 날씨 + 일출/일몰 응답.
 * Open-Meteo 무료 API(키 불필요) 응답을 가공한 결과.
 *
 * @param lat                위도
 * @param lng                경도
 * @param currentTempC       현재 기온(섭씨). 조회 실패 시 null
 * @param currentDescription 현재 날씨 한글 설명 (예: "맑음"). 조회 실패 시 "정보 없음"
 * @param daily              일별 예보 (오늘 포함 최대 3일)
 */
public record WeatherResponse(
        double lat,
        double lng,
        Double currentTempC,
        String currentDescription,
        List<DailyForecast> daily
) {

    /**
     * 일별 예보 항목.
     *
     * @param date        날짜 (yyyy-MM-dd)
     * @param sunrise     일출 시각 (ISO-8601, 예: "2026-06-19T05:11")
     * @param sunset      일몰 시각 (ISO-8601)
     * @param minTempC    최저 기온(섭씨)
     * @param maxTempC    최고 기온(섭씨)
     * @param description 한글 날씨 설명
     */
    public record DailyForecast(
            String date,
            String sunrise,
            String sunset,
            Double minTempC,
            Double maxTempC,
            String description
    ) {}
}
