// Created: 2026-06-15 21:48:32
package com.trip.community.dto;

import com.trip.community.entity.enums.PostCategory;

import java.util.List;

public record PostUpdateRequest(
        String title,
        String content,
        PostCategory category,
        List<String> imageUrls  // null이면 기존 이미지 유지
) {}
