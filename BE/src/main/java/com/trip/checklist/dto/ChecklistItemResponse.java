package com.trip.checklist.dto;

import com.trip.checklist.entity.ChecklistItem;

/**
 * 체크리스트 항목 응답 DTO.
 */
public record ChecklistItemResponse(
        Long id,
        Long planId,
        Integer dayNo,
        String title,
        String category,
        boolean checked,
        int sortOrder
) {
    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getPlanId(),
                item.getDayNo(),
                item.getTitle(),
                item.getCategory(),
                item.isChecked(),
                item.getSortOrder()
        );
    }
}
