package com.trip.plan.dto;

import com.trip.plan.entity.CompanionsType;
import com.trip.plan.entity.OriginType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlanCreateRequestDto(
        @NotBlank @Size(max = 100)
        String title,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        CompanionsType companions,

        Integer budget,

        OriginType origin,

        // 대표 이미지 URL(선택). 없으면 프론트에서 기본 이미지로 표시.
        @Size(max = 500)
        String imageUrl
) {}
