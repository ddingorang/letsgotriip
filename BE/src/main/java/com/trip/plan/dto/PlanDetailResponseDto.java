package com.trip.plan.dto;

import com.trip.plan.entity.CompanionsType;
import com.trip.plan.entity.OriginType;
import com.trip.plan.entity.TripPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PlanDetailResponseDto(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        CompanionsType companions,
        Integer budget,
        OriginType origin,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DayResponseDto> days
) {
    public static PlanDetailResponseDto from(TripPlan plan) {
        return new PlanDetailResponseDto(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getCompanions(),
                plan.getBudget(),
                plan.getOrigin(),
                plan.getVersion(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getDays().stream().map(DayResponseDto::from).toList()
        );
    }
}
