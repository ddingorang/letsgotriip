// Created: 2026-06-15 22:32:57
package com.trip.user.dto;

import com.trip.user.entity.AlbumPhoto;

public record AlbumPhotoResponse(
        Long id,
        String imageUrl,
        int displayOrder
) {
    public static AlbumPhotoResponse from(AlbumPhoto photo) {
        return new AlbumPhotoResponse(photo.getId(), photo.getImageUrl(), photo.getDisplayOrder());
    }
}
