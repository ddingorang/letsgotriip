package com.trip.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import com.trip.user.entity.BaseEntity;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_membership_room_user",
        columnNames = {"chatroom_id", "user_id"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id")
    private ChatRoom chatRoom;

    @Column(name = "is_host", nullable = false)
    private Boolean isHost;

    @CreatedDate
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "join_message_id")
    private Long joinMessageId;

    /**
     * 본인 음소거(알림 끄기) 여부. 멤버 본인이 토글한다.
     * NOTE: 기존 행/기존 빌더(companion 도메인)는 이 값을 세팅하지 않으므로
     * DB default(false)를 부여하고, 읽기는 isMuted()로 null-safe 처리한다.
     */
    @Column(name = "muted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean muted = false;

    /** 채팅방 나가기 — 탈퇴 시각을 기록한다(소프트 탈퇴). */
    public void leave(LocalDateTime when) {
        this.leftAt = when;
    }

    /** 현재 방에 활성 상태로 참여 중인지(나가지 않았고 강퇴되지 않음). */
    public boolean isActiveMember() {
        return this.leftAt == null && Boolean.FALSE.equals(this.isBanned);
    }

    /** 음소거 여부(null 안전). */
    public boolean isMuted() {
        return Boolean.TRUE.equals(this.muted);
    }

    /** 본인 음소거 토글. */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    /** 방장(host)에 의한 강퇴 — 강퇴 플래그/시각과 탈퇴 시각을 함께 기록한다. */
    public void ban(LocalDateTime when) {
        this.isBanned = true;
        this.bannedAt = when;
        this.leftAt = when;
    }

    /**
     * 강퇴/탈퇴 상태를 해제하고 활성 멤버로 되돌린다(재초대 시 사용).
     * 새로 입장한 것으로 보아 음소거는 유지하지 않고 해제한다.
     */
    public void reactivate() {
        this.isBanned = false;
        this.bannedAt = null;
        this.leftAt = null;
        this.muted = false;
    }

    /** 방장 권한 부여/회수(위임 시 사용). */
    public void changeHost(boolean host) {
        this.isHost = host;
    }
}