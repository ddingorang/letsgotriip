package com.trip.user.dto;

import com.trip.user.entity.enums.Gender;
import com.trip.user.entity.enums.UserRole;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserProfileResponseDto(
        Long userId,
        String email,
        String nickname,
        String name,
        Gender gender,
        LocalDate birthDate,
        String profileImageUrl,
        UserRole userRole
) {
}
