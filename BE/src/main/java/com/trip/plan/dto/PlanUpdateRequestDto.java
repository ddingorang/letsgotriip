package com.trip.plan.dto;

import com.trip.plan.entity.CompanionsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlanUpdateRequestDto(
        @NotNull
        Long expectedVersion,

        @NotBlank @Size(max = 100)
        String title,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        CompanionsType companions,

        Integer budget
) {}
