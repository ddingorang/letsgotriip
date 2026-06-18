// Created: 2026-06-15 22:58:09
package com.trip.community.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {}
