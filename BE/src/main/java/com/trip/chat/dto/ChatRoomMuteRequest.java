package com.trip.chat.dto;

import jakarta.validation.constraints.NotNull;

/** 본인 멤버십 음소거 토글 요청. */
public record ChatRoomMuteRequest(
        @NotNull Boolean muted
) {
}
