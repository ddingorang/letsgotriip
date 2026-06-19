package com.trip.checklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 체크리스트 항목 생성 요청 DTO.
 */
public record ChecklistItemCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 20) String category,
        Long planId,
        Integer dayNo,
        Integer sortOrder
) {}
