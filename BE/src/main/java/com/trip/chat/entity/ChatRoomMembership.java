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

    /** 채팅방 나가기 — 탈퇴 시각을 기록한다(소프트 탈퇴). */
    public void leave(LocalDateTime when) {
        this.leftAt = when;
    }

    /** 현재 방에 활성 상태로 참여 중인지(나가지 않았고 강퇴되지 않음). */
    public boolean isActiveMember() {
        return this.leftAt == null && Boolean.FALSE.equals(this.isBanned);
    }
}