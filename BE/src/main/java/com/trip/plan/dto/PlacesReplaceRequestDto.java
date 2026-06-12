package com.trip.plan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlacesReplaceRequestDto(
        @NotNull
        Long expectedVersion,

        @NotNull
        List<PlaceItemDto> places
) {}
