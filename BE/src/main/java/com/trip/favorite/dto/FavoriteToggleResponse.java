package com.trip.favorite.dto;

/**
 * 토글 결과 — favorited=true면 추가됨, false면 해제됨.
 */
public record FavoriteToggleResponse(boolean favorited) {
}
