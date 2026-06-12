package com.trip.user.dto;

public record SignupRequestDto(

        String nickname,
        String email,
        String password,
        String profileImageUrl
) {
}
