package com.trip.notice.dto;

import jakarta.validation.constraints.NotBlank;

/** 공지 수정 요청 (admin). */
public record NoticeUpdateRequest(

        String category,

        @NotBlank
        String title,

        @NotBlank
        String content,

        boolean pinned
) {
}
