package com.trip.plan.dto;

/**
 * 공유 활성화 응답.
 * - shareToken: 발급된 공개 토큰 (idempotent — 이미 발급된 경우 재사용)
 * - shareUrl: 프론트 공유 경로 ("/plan/shared/{token}")
 */
public record PlanShareResponseDto(
        String shareToken,
        String shareUrl
) {
    public static PlanShareResponseDto of(String token) {
        return new PlanShareResponseDto(token, "/plan/shared/" + token);
    }
}
