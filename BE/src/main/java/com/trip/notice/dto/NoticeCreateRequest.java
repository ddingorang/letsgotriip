package com.trip.notice.dto;

import jakarta.validation.constraints.NotBlank;

/** 공지 생성 요청 (admin). */
public record NoticeCreateRequest(

        String category,

        @NotBlank
        String title,

        @NotBlank
        String content,

        boolean pinned
) {
}
