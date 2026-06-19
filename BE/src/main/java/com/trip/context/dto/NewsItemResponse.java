package com.trip.context.dto;

/**
 * 여행 뉴스 항목 응답 (데모 데이터).
 *
 * @param title       제목
 * @param summary     요약
 * @param source      출처
 * @param url         원문 링크
 * @param publishedAt 발행 시각 (ISO-8601 또는 yyyy-MM-dd)
 * @param demo        데모 데이터 여부 (항상 true)
 */
public record NewsItemResponse(
        String title,
        String summary,
        String source,
        String url,
        String publishedAt,
        boolean demo
) {}
