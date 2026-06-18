// Created: 2026-06-15 21:48:30
package com.trip.community.dto;

import com.trip.community.entity.enums.PostCategory;

import java.util.List;

public record PostCreateRequest(
        String title,
        String content,
        PostCategory category,
        List<String> imageUrls
) {}
