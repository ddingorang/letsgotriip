package com.trip.user.dto;

public record UserUpdateRequestDto(
        String nickname,
        String profileImageUrl,
        String bio
) {
}
