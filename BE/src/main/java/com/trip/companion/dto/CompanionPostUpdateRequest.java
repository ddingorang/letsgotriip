// Created: 2026-06-15 23:42:35
package com.trip.companion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompanionPostUpdateRequest(
        @Size(max = 200) String title,
        LocalDate travelDate,
        @Size(max = 100) String region,
        @Size(max = 50) String duration,
        @Min(1) @Max(50) Integer maxMembers,
        @Min(0) Integer estimatedCost,
        @Size(max = 2000) String description
) {}
