package com.trip.user.dto;

import java.util.List;

/**
 * 온보딩 취향설문 저장 요청.
 * - interests: 관심사 key 목록 (예: ["nature", "food", "activity"])
 * - companion: 동행 유형 (예: "혼자", "연인", "친구", "가족")
 */
public record PreferenceUpdateRequestDto(
        List<String> interests,
        String companion
) {
}
