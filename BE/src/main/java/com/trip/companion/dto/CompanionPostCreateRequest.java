// Created: 2026-06-15 23:42:34
package com.trip.companion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CompanionPostCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate travelDate,
        @NotBlank @Size(max = 100) String region,
        @Size(max = 50) String duration,
        @Min(1) @Max(50) int maxMembers,
        @Min(0) int estimatedCost,
        @Size(max = 2000) String description,
        @Size(max = 20) List<@Size(max = 30) String> tags
) {}
