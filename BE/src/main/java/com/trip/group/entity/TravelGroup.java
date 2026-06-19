package com.trip.group.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_groups")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private int maxMembers = 10;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.maxMembers <= 0) this.maxMembers = 10;
    }

    /** 그룹 이름 변경 */
    public void rename(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }

    /** 그룹 정보 수정 (이름/설명/최대 인원) */
    public void update(String name, String description, Integer maxMembers) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (maxMembers != null && maxMembers > 0) this.maxMembers = maxMembers;
    }
}
