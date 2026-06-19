package com.trip.checklist.dto;

import jakarta.validation.constraints.Size;

/**
 * 체크리스트 항목 부분 수정 요청 DTO — 모든 필드 선택(null은 기존 값 유지).
 */
public record ChecklistItemUpdateRequest(
        @Size(max = 200) String title,
        @Size(max = 20) String category,
        Boolean checked,
        Integer sortOrder
) {}
