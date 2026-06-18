// Created: 2026-06-15 22:33:01
package com.trip.user.dto;

import java.util.List;

public record AlbumCreateRequest(
        String name,
        List<String> imageUrls
) {}
