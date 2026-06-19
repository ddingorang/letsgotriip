package com.trip.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재설정 요청 (토큰 발급). */
public record PasswordResetRequest(

        @NotBlank
        @Email
        String email
) {
}
