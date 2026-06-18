// Created: 2026-06-15 23:42:04
package com.trip.companion.entity;

import com.trip.companion.entity.enums.ApplicationStatus;
import com.trip.user.entity.BaseEntity;
import com.trip.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companion_applications")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_post_id", nullable = false)
    private CompanionPost companionPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    public void approve() {
        this.status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        this.status = ApplicationStatus.REJECTED;
    }
}
