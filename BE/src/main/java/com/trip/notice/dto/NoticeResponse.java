package com.trip.notice.dto;

import com.trip.notice.entity.Notice;

import java.time.LocalDateTime;

/**
 * 공지 응답 DTO. 목록·상세 공용 (content 포함).
 */
public record NoticeResponse(
        Long id,
        String category,
        String title,
        String content,
        boolean pinned,
        LocalDateTime createdAt
) {
    public static NoticeResponse from(Notice n) {
        return new NoticeResponse(
                n.getId(),
                n.getCategory(),
                n.getTitle(),
                n.getContent(),
                n.isPinned(),
                n.getCreatedAt()
        );
    }
}
