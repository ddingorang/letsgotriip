// Created: 2026-06-15 23:42:35
package com.trip.companion.dto;

import java.time.LocalDate;

public record CompanionPostUpdateRequest(
        String title,
        LocalDate travelDate,
        String region,
        String duration,
        Integer maxMembers,
        Integer estimatedCost,
        String description
) {}
