package com.trip.group.dto;

import com.trip.group.entity.TravelGroup;

public record GroupResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        int memberCount,
        int maxMembers
) {
    public static GroupResponse from(TravelGroup group, int memberCount) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getOwnerId(),
                memberCount,
                group.getMaxMembers()
        );
    }
}
