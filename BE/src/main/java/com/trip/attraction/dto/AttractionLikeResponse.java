package com.trip.attraction.dto;

/**
 * 좋아요 토글/상태 응답 — { liked, likeCount }.
 * liked  : 현재 사용자의 좋아요 여부(비로그인 조회 시 false)
 * likeCount: 공개 인기 카운터(Attraction.likeCount)
 */
public record AttractionLikeResponse(boolean liked, int likeCount) {
}
