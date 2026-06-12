package com.trip.plan.dto;

import com.trip.plan.entity.TripPlace;

import java.time.LocalTime;

public record PlaceResponseDto(
        Long id,
        Integer seq,
        LocalTime visitTime,
        String memo,
        AttractionSummaryDto attraction
) {
    public static PlaceResponseDto from(TripPlace place) {
        var a = place.getAttraction();
        return new PlaceResponseDto(
                place.getId(),
                place.getSeq(),
                place.getVisitTime(),
                place.getMemo(),
                new AttractionSummaryDto(
                        a.getId(),
                        a.getContentId(),
                        a.getContentType(),
                        a.getTitle(),
                        a.getAddr(),
                        a.getLatitude(),
                        a.getLongitude(),
                        a.getImageUrl()
                )
        );
    }
}
