// Created: 2026-06-15 22:32:40
package com.trip.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "albums")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 공개 공유 토큰 (G12). null이면 미공유 상태.
     * 공유 활성화 시 UUID 기반 토큰을 1회 발급하고 이후 재사용한다(idempotent).
     * unique 제약으로 토큰 충돌 방지. (ddl-auto=update가 컬럼을 추가)
     */
    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    public void rename(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }

    /** 공유 토큰 발급. 이미 발급된 경우 호출 측에서 재사용하므로 여기서는 단순 세팅만. */
    public void markShared(String token) {
        this.shareToken = token;
    }
}
