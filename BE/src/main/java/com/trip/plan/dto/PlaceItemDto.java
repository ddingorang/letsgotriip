package com.trip.plan.dto;

import java.time.LocalTime;

/**
 * replacePlaces 요청의 개별 장소 항목.
 * placeId(기존 TripPlace id) 또는 contentId+contentType(신규 스냅샷) 중 하나 사용.
 */
public record PlaceItemDto(
        Long placeId,
        String contentId,
        Integer contentType,
        Integer seq,
        LocalTime visitTime,
        String memo
) {}
