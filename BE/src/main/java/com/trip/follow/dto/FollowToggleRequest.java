package com.trip.follow.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 팔로우 토글 요청 — 대상 사용자 id.
 */
public record FollowToggleRequest(
        @NotNull Long targetUserId
) {
}
