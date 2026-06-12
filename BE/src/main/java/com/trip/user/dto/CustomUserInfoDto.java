package com.trip.user.dto;

import lombok.Builder;

import com.trip.user.entity.enums.UserRole;

@Builder
public record CustomUserInfoDto(

        Long userId,
        String email,
        String nickname,
        UserRole userRole
) {
}
