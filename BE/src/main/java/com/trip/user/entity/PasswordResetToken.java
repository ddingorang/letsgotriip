package com.trip.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 토큰 (데모용).
 * 실제 운영에서는 이메일로 전송되지만, 데모에서는 응답으로 토큰을 직접 노출한다.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Builder
    public PasswordResetToken(String token, Long userId, LocalDateTime expiresAt, boolean used) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    /** 만료 여부 (기준 시각 이후면 만료) */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }

    /** 사용 완료 처리 */
    public void markUsed() {
        this.used = true;
    }
}
