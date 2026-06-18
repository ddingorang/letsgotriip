package com.trip.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequestDto(

        @NotBlank
        @Size(max = 20)
        String nickname,

        @NotBlank
        @Email
        @Size(max = 50)
        String email,

        @NotBlank
        @Size(max = 60)
        String password,

        String profileImageUrl
) {
}
