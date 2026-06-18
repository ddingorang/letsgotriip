// Created: 2026-06-15 23:25:09
package com.trip.community.dto;

import com.trip.community.entity.HotPlace;
import com.trip.community.entity.enums.HotPlaceCategory;
import com.trip.community.entity.enums.HotPlaceStatus;

import java.time.LocalDateTime;
import java.util.List;

public record HotPlaceResponse(
        Long id,
        String submitterNickname,
        String name,
        String address,
        Double latitude,
        Double longitude,
        HotPlaceCategory category,
        String description,
        HotPlaceStatus status,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static HotPlaceResponse of(HotPlace hotPlace, List<String> imageUrls) {
        return new HotPlaceResponse(
                hotPlace.getId(),
                hotPlace.getSubmitter().getNickname(),
                hotPlace.getName(),
                hotPlace.getAddress(),
                hotPlace.getLatitude(),
                hotPlace.getLongitude(),
                hotPlace.getCategory(),
                hotPlace.getDescription(),
                hotPlace.getStatus(),
                imageUrls,
                hotPlace.getCreatedAt()
        );
    }
}
