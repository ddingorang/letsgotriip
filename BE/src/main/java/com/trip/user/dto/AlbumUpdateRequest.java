// Created: 2026-06-15 22:33:02
package com.trip.user.dto;

import java.util.List;

public record AlbumUpdateRequest(
        String name,
        List<String> addImageUrls,
        List<Long> removePhotoIds
) {}
