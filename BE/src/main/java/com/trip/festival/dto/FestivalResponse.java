// Created: 2026-06-08 15:32:19
package com.trip.festival.dto;

import com.trip.festival.entity.Festival;

public record FestivalResponse(
        String contentId,
        String title,
        String address,
        String tel,
        String imageUrl,
        String startDate,
        String endDate,
        Double latitude,
        Double longitude,
        String areaCode,
        String sigunguCode,
        String status
) {
    public static FestivalResponse from(Festival f) {
        return new FestivalResponse(
                f.getContentId(),
                f.getTitle(),
                f.getAddress(),
                f.getTel(),
                f.getImageUrl(),
                f.getStartDate() != null ? f.getStartDate().toString() : null,
                f.getEndDate()   != null ? f.getEndDate().toString()   : null,
                f.getLatitude(),
                f.getLongitude(),
                f.getAreaCode(),
                f.getSigunguCode(),
                f.getStatus() != null ? f.getStatus().name() : null
        );
    }
}
