// Created: 2026-06-15 23:42:04
package com.trip.companion.entity;

import com.trip.companion.entity.enums.ApplicationStatus;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
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

    /** 신청자가 방장에게 남기는 신청 메시지 (선택) */
    @Column(length = 500)
    private String message;

    public boolean isPending() {
        return this.status == ApplicationStatus.PENDING;
    }

    public void approve() {
        if (!isPending()) throw new GeneralException(ResponseCode.COMPANION_ALREADY_PROCESSED);
        this.status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        if (!isPending()) throw new GeneralException(ResponseCode.COMPANION_ALREADY_PROCESSED);
        this.status = ApplicationStatus.REJECTED;
    }
}
