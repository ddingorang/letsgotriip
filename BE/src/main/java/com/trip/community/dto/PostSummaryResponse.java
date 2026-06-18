// Created: 2026-06-15 23:26:06
package com.trip.community.dto;

import com.trip.community.entity.Post;
import com.trip.community.entity.enums.PostCategory;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String authorNickname,
        String authorProfileImageUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        PostCategory category,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse of(Post post, int commentCount, String thumbnailUrl) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getNickname(),
                post.getAuthor().getProfileImageUrl(),
                post.getViewCount(),
                post.getLikeCount(),
                commentCount,
                post.getCategory(),
                thumbnailUrl,
                post.getCreatedAt()
        );
    }
}
