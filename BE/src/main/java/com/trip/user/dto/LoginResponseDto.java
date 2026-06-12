package com.trip.user.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        Long userId,
        String accessToken,
        String refreshToken,
        String familyId // sessionId
) {
}
