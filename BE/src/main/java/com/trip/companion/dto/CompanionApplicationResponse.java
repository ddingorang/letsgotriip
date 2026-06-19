// Created: 2026-06-15 23:42:47
package com.trip.companion.dto;

import com.trip.companion.entity.CompanionApplication;
import com.trip.companion.entity.enums.ApplicationStatus;
import com.trip.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record CompanionApplicationResponse(
        Long id,
        Long applicantId,
        String applicantNickname,
        String applicantProfileImageUrl,
        String ageGroup,        // 신청자 연령대 (예: "20대"). birthDate 없으면 null
        Integer tripCount,      // 동행 횟수 통계 미보유 → null (엔티티에 통계 필드 부재)
        Integer mannerScore,    // 매너 점수 통계 미보유 → null
        String message,         // 신청 메시지 (실데이터)
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static CompanionApplicationResponse of(CompanionApplication application) {
        User applicant = application.getApplicant();
        return new CompanionApplicationResponse(
                application.getId(),
                applicant.getId(),
                applicant.getNickname(),
                applicant.getProfileImageUrl(),
                toAgeGroup(applicant.getBirthDate()),
                null,
                null,
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }

    /** 생년월일 → 연령대 라벨("20대" 등). 정보 없으면 null. */
    private static String toAgeGroup(LocalDate birthDate) {
        if (birthDate == null) return null;
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 10) return null;
        return (age / 10) * 10 + "대";
    }
}
