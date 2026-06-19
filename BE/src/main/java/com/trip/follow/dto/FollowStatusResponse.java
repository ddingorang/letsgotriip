package com.trip.follow.dto;

/**
 * 팔로우 상태 응답 — 요청자 기준 isFollowing + 대상 사용자의 팔로워/팔로잉 수.
 */
public record FollowStatusResponse(
        Long userId,
        boolean following,
        long followerCount,
        long followingCount
) {
}
