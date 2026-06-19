package com.trip.user.dto;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 토큰 응답 (데모용).
 * 계정 열거 방지를 위해 이메일이 존재하지 않아도 동일한 형태로 응답하며,
 * 이 경우 token/expiresAt 은 null 이다.
 */
public record PasswordResetTokenResponse(
        String token,
        LocalDateTime expiresAt,
        String demoNote
) {
}
