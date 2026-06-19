// Created: 2026-06-20
package com.trip.user.dto;

/**
 * 앨범 공유 활성화 응답 (G12).
 * - token: 발급된 공개 토큰 (idempotent — 이미 발급된 경우 재사용)
 */
public record AlbumShareResponse(
        String token
) {
    public static AlbumShareResponse of(String token) {
        return new AlbumShareResponse(token);
    }
}
