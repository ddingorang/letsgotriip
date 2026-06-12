package com.trip.user.dto;

import lombok.Builder;

@Builder
public record AccessTokenResponseDto(
        String accessToken,
        Long userId
) {
}
