package com.trip.user.dto;

import lombok.Builder;

@Builder
public record SignupResponseDto(
        Long userId,
        String email
) {
}
