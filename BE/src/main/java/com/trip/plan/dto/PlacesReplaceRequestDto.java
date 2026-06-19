package com.trip.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlacesReplaceRequestDto(
        @NotNull
        Long expectedVersion,

        // @Valid: 각 PlaceItemDto의 제약(seq @NotNull 등)을 재귀 검증한다.
        // 누락 시 잘못된 항목이 검증을 통과해 DB NOT NULL 위반 500으로 새므로 반드시 필요.
        @NotNull
        @Valid
        List<PlaceItemDto> places
) {}
