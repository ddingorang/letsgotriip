package com.trip.notification.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 알림 — 이벤트(동행 신청·댓글 등) 발생 시 수신자에게 적재.
 * 생성 시각은 BaseEntity(createdAt)에서 관리.
 */
@Entity
@Table(name = "notifications_user",
        indexes = @Index(name = "idx_noti_recipient", columnList = "recipient_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받을 사용자 id */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /** companion / community / badge / system */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 300)
    private String body;

    /** FE 딥링크 경로 (예: /community/companion) — 선택 */
    @Column(length = 200)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Builder
    public Notification(Long recipientId, String type, String title, String body, String link) {
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.read = false;
    }

    public void markRead() {
        this.read = true;
    }
}
