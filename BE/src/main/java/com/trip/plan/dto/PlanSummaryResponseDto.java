package com.trip.plan.dto;

import com.trip.plan.entity.CompanionsType;
import com.trip.plan.entity.OriginType;
import com.trip.plan.entity.PlanStatus;
import com.trip.plan.entity.TripPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlanSummaryResponseDto(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        CompanionsType companions,
        Integer budget,
        OriginType origin,
        PlanStatus status,
        String imageUrl,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PlanSummaryResponseDto from(TripPlan plan) {
        return new PlanSummaryResponseDto(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getCompanions(),
                plan.getBudget(),
                plan.getOrigin(),
                plan.getStatus(),
                plan.getImageUrl(),
                plan.getVersion(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
