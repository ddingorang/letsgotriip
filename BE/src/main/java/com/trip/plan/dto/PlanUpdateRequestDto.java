package com.trip.plan.dto;

import com.trip.plan.entity.CompanionsType;
import com.trip.plan.entity.PlanStatus;
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

        Integer budget,

        // 대표 이미지 URL. null이면 미변경, ""이면 제거, 값이면 교체.
        @Size(max = 500)
        String imageUrl,

        PlanStatus status
) {}
