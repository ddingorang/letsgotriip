package com.trip.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 추천 요청 DTO.
 * areaCode 필수, 기간은 7일 이내 (startDate ~ endDate 포함).
 */
public record RecommendRequestDto(
        @NotBlank
        String areaCode,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        String companions,

        Integer budget,

        List<String> themes,

        String title
) {
    /**
     * 기간 유효성: startDate <= endDate, 간격 <= 6일 (최대 7일 포함)
     */
    public boolean isPeriodValid() {
        if (startDate == null || endDate == null) return false;
        if (startDate.isAfter(endDate)) return false;
        long days = endDate.toEpochDay() - startDate.toEpochDay();
        return days <= 6; // 7일 이내: 0~6 차이
    }

    public int totalDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
