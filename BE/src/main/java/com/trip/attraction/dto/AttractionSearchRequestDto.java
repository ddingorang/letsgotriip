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
        int size,
        String mapX,      // 경도(lng) — 좌표 기반 검색 시
        String mapY,      // 위도(lat) — 좌표 기반 검색 시
        Integer radius    // 반경(m)
) {
    /** size를 1~50 범위로 clamp */
    public int clampedSize() {
        return Math.min(Math.max(size, 1), 50);
    }

    /** 좌표 기반(내 위치 근처) 검색 여부 */
    public boolean hasCoords() {
        return mapX != null && !mapX.isBlank() && mapY != null && !mapY.isBlank();
    }

    /** radius(m)를 1~20000 범위로 clamp, 미지정 시 5000m */
    public int clampedRadius() {
        int r = radius == null ? 5000 : radius;
        return Math.min(Math.max(r, 1), 20000);
    }
}
