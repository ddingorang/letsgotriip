// Created: 2026-06-15 23:25:11
package com.trip.community.dto;

import com.trip.community.entity.HotPlace;
import com.trip.community.entity.enums.HotPlaceCategory;

import java.time.LocalDateTime;

public record HotPlaceSummaryResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        HotPlaceCategory category,
        String thumbnailUrl,
        int likeCount,
        Double rating,
        int ratingCount,
        LocalDateTime createdAt
) {
    public static HotPlaceSummaryResponse of(HotPlace hotPlace, String thumbnailUrl) {
        return new HotPlaceSummaryResponse(
                hotPlace.getId(),
                hotPlace.getName(),
                hotPlace.getAddress(),
                hotPlace.getLatitude(),
                hotPlace.getLongitude(),
                hotPlace.getCategory(),
                thumbnailUrl,
                hotPlace.getLikeCount(),
                hotPlace.getRating(),
                hotPlace.getRatingCount(),
                hotPlace.getCreatedAt()
        );
    }
}
