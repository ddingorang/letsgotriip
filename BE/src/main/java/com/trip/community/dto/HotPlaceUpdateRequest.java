// Created: 2026-06-15 23:25:06
package com.trip.community.dto;

import com.trip.community.entity.enums.HotPlaceCategory;

import java.util.List;

public record HotPlaceUpdateRequest(
        String name,
        String address,
        Double latitude,
        Double longitude,
        HotPlaceCategory category,
        String description,
        List<String> imageUrls
) {}
