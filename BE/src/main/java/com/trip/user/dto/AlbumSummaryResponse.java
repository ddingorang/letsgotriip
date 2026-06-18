// Created: 2026-06-15 22:33:00
package com.trip.user.dto;

import com.trip.user.entity.Album;

import java.time.LocalDateTime;

public record AlbumSummaryResponse(
        Long id,
        String name,
        int photoCount,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
    public static AlbumSummaryResponse of(Album album, int photoCount, String thumbnailUrl) {
        return new AlbumSummaryResponse(
                album.getId(),
                album.getName(),
                photoCount,
                thumbnailUrl,
                album.getCreatedAt()
        );
    }
}
