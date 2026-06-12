package com.trip.plan.dto;

import com.trip.plan.entity.TripDay;

import java.util.List;

public record DayResponseDto(
        Long id,
        Integer dayNo,
        String memo,
        List<PlaceResponseDto> places
) {
    public static DayResponseDto from(TripDay day) {
        return new DayResponseDto(
                day.getId(),
                day.getDayNo(),
                day.getMemo(),
                day.getPlaces().stream().map(PlaceResponseDto::from).toList()
        );
    }
}
