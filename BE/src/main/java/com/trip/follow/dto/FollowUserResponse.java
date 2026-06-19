package com.trip.follow.dto;

import com.trip.user.entity.User;

/**
 * 팔로잉 목록 항목 — 내가 팔로우한 사용자의 요약 프로필.
 */
public record FollowUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String bio
) {
    public static FollowUserResponse from(User user) {
        return new FollowUserResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getBio()
        );
    }
}
