package com.trip.attraction.dto;

/**
 * GET /api/attractions 검색 파라미터
 */
public record AttractionSearchRequestDto(
        String areaCode,
        String sigunguCode,
        String contentTypeId,
        String keyword,
        int page,
        int size
) {
    /** size를 1~50 범위로 clamp */
    public int clampedSize() {
        return Math.min(Math.max(size, 1), 50);
    }
}
