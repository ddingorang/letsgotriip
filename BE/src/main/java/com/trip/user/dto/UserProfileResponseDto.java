package com.trip.user.dto;

import com.trip.user.entity.enums.Gender;
import com.trip.user.entity.enums.UserRole;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record UserProfileResponseDto(
        Long userId,
        String email,
        String nickname,
        String name,
        Gender gender,
        LocalDate birthDate,
        String profileImageUrl,
        String bio,
        List<String> preferredInterests,
        String preferredCompanion,
        UserRole userRole
) {
}
