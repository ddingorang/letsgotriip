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
}