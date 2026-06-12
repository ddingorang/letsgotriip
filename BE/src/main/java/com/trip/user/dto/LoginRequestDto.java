package com.trip.user.dto;

// TODO: 검증 로직 필요
public record LoginRequestDto(
        String email,
        String password
) {
}
