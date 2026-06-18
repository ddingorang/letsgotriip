// Created: 2026-06-15 23:42:02
package com.trip.companion.entity;

import com.trip.chat.entity.ChatRoom;
import com.trip.companion.entity.enums.CompanionStatus;
import com.trip.user.entity.BaseEntity;
import com.trip.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "companion_posts")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate travelDate;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false, length = 50)
    private String duration;

    @Column(nullable = false)
    private int maxMembers;

    @Column(nullable = false)
    private int estimatedCost;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanionStatus status = CompanionStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public void update(String title, LocalDate travelDate, String region, String duration,
                       Integer maxMembers, Integer estimatedCost, String description) {
        if (title != null && !title.isBlank()) this.title = title;
        if (travelDate != null) this.travelDate = travelDate;
        if (region != null && !region.isBlank()) this.region = region;
        if (duration != null && !duration.isBlank()) this.duration = duration;
        if (maxMembers != null) this.maxMembers = maxMembers;
        if (estimatedCost != null) this.estimatedCost = estimatedCost;
        if (description != null && !description.isBlank()) this.description = description;
    }

    public void delete() {
        this.deleted = true;
    }

    public void close() {
        this.status = CompanionStatus.CLOSED;
    }

    public void assignChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }
}
