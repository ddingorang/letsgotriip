// Created: 2026-06-19
package com.trip.companion.dto;

import jakarta.validation.constraints.Size;

/**
 * 동행 신청 요청 바디. 신청 메시지는 선택값(없으면 null).
 */
public record CompanionApplicationRequest(
        @Size(max = 500) String message
) {}
