package com.trip.follow.dto;

/**
 * 팔로우 토글 결과 — following=true면 팔로우, false면 해제됨.
 * 갱신된 대상 사용자의 팔로워 수를 함께 반환.
 */
public record FollowToggleResponse(
        Long targetUserId,
        boolean following,
        long followerCount
) {
}
