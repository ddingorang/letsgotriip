// Created: 2026-06-15 23:42:34
package com.trip.companion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CompanionPostCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate travelDate,
        @NotBlank @Size(max = 100) String region,
        @Size(max = 50) String duration,
        @Min(1) @Max(50) int maxMembers,
        @Min(0) int estimatedCost,
        @Size(max = 2000) String description,
        @Size(max = 20) List<@Size(max = 30) String> tags,
        Long planId   // optional — 연결할 작성자 소유 여행 계획 ID. null이면 미연결
) {
    /** planId 없는 기존 호출(시드 등) 호환용 보조 생성자 — planId=null. */
    public CompanionPostCreateRequest(String title, LocalDate travelDate, String region,
                                      String duration, int maxMembers, int estimatedCost,
                                      String description, List<String> tags) {
        this(title, travelDate, region, duration, maxMembers, estimatedCost, description, tags, null);
    }
}
