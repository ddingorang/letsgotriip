// Created: 2026-06-15 22:32:59
package com.trip.user.dto;

import com.trip.user.entity.Album;

import java.time.LocalDateTime;
import java.util.List;

public record AlbumResponse(
        Long id,
        String name,
        List<AlbumPhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AlbumResponse of(Album album, List<AlbumPhotoResponse> photos) {
        return new AlbumResponse(
                album.getId(),
                album.getName(),
                photos,
                album.getCreatedAt(),
                album.getUpdatedAt()
        );
    }
}
