package com.trip.attraction.dto;

import com.trip.attraction.entity.Attraction;

/**
 * 큐레이션 조회 응답 항목.
 * tags 는 저장 형태(",food,night,")에서 앞뒤 구분 콤마를 제거해 "food,night" 로 노출한다.
 */
public record CuratedAttractionResponse(
        String contentId,
        Integer contentType,
        String title,
        String addr,
        String imageUrl,
        Double latitude,
        Double longitude,
        int likeCount,
        String tags
) {
    public static CuratedAttractionResponse from(Attraction a) {
        return new CuratedAttractionResponse(
                a.getContentId(),
                a.getContentType(),
                a.getTitle(),
                a.getAddr(),
                a.getImageUrl(),
                a.getLatitude(),
                a.getLongitude(),
                a.getLikeCount(),
                normalizeTags(a.getTags())
        );
    }

    /** ",food,night," → "food,night" (앞뒤 콤마 제거). null/빈 값은 빈 문자열. */
    private static String normalizeTags(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String t = raw;
        while (t.startsWith(",")) t = t.substring(1);
        while (t.endsWith(","))   t = t.substring(0, t.length() - 1);
        return t;
    }
}
