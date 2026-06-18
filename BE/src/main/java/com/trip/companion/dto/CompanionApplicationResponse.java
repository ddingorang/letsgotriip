// Created: 2026-06-15 23:42:47
package com.trip.companion.dto;

import com.trip.companion.entity.CompanionApplication;
import com.trip.companion.entity.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record CompanionApplicationResponse(
        Long id,
        Long applicantId,
        String applicantNickname,
        String applicantProfileImageUrl,
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static CompanionApplicationResponse of(CompanionApplication application) {
        return new CompanionApplicationResponse(
                application.getId(),
                application.getApplicant().getId(),
                application.getApplicant().getNickname(),
                application.getApplicant().getProfileImageUrl(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}
