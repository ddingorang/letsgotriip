package com.trip.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record PlaceAddRequestDto(
        @NotBlank
        String contentId,

        @NotNull
        Integer contentType,

        LocalTime visitTime,

        String memo
) {}
