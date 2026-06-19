package com.trip.checklist.dto;

import java.util.List;

/**
 * 내장 체크리스트 템플릿 응답 DTO.
 */
public record ChecklistTemplateResponse(
        String key,
        String name,
        List<Item> items
) {
    /** 템플릿에 포함된 개별 항목 */
    public record Item(
            String title,
            String category
    ) {}
}
