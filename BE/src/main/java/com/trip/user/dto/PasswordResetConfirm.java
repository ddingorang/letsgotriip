package com.trip.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비밀번호 재설정 확정 (토큰 + 새 비밀번호). */
public record PasswordResetConfirm(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 60)
        String newPassword
) {
}
