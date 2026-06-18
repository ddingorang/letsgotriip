// Created: 2026-06-15 23:25:05
package com.trip.community.dto;

import com.trip.community.entity.enums.HotPlaceCategory;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HotPlaceCreateRequest(
        String name,
        String address,
        Double latitude,
        Double longitude,
        @NotNull HotPlaceCategory category,
        String description,
        List<String> imageUrls
) {}
