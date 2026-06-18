// Created: 2026-06-15 23:42:34
package com.trip.companion.dto;

import java.time.LocalDate;

public record CompanionPostCreateRequest(
        String title,
        LocalDate travelDate,
        String region,
        String duration,
        int maxMembers,
        int estimatedCost,
        String description
) {}
