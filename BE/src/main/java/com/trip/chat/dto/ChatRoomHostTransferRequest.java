package com.trip.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 방장 위임 요청 — 새 방장이 될 활성 멤버의 userId. */
public record ChatRoomHostTransferRequest(
        @NotNull @Positive Long newHostUserId
) {
}
