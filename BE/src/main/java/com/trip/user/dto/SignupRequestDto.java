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
        @Size(min = 8, max = 60, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        String profileImageUrl
) {
}
