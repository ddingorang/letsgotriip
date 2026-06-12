package com.trip.plan.dto;

/**
 * 장소 응답 내에 포함되는 관광지 요약 정보
 */
public record AttractionSummaryDto(
        Long id,
        String contentId,
        Integer contentType,
        String title,
        String addr,
        Double latitude,
        Double longitude,
        String imageUrl
) {}
