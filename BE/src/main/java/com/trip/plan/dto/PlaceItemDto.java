package com.trip.plan.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * replacePlaces 요청의 개별 장소 항목.
 * placeId(기존 TripPlace id) 또는 contentId+contentType(신규 스냅샷) 중 하나 사용.
 */
public record PlaceItemDto(
        Long placeId,
        String contentId,
        Integer contentType,
        // seq는 DB NOT NULL 컬럼이므로 null이면 500 대신 400으로 거른다.
        @NotNull
        Integer seq,
        LocalTime visitTime,
        String memo
) {}
