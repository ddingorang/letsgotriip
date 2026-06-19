// Created: 2026-06-15 23:26:06
package com.trip.community.dto;

import com.trip.community.entity.Post;
import com.trip.community.entity.enums.PostCategory;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        Long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        PostCategory category,
        String thumbnailUrl,
        boolean likedByMe,
        boolean bookmarked,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse of(Post post, int commentCount, String thumbnailUrl) {
        return of(post, commentCount, thumbnailUrl, false, false);
    }

    public static PostSummaryResponse of(Post post, int commentCount, String thumbnailUrl, boolean likedByMe) {
        return of(post, commentCount, thumbnailUrl, likedByMe, false);
    }

    public static PostSummaryResponse of(Post post, int commentCount, String thumbnailUrl, boolean likedByMe, boolean bookmarked) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getAuthor().getProfileImageUrl(),
                post.getViewCount(),
                post.getLikeCount(),
                commentCount,
                post.getCategory(),
                thumbnailUrl,
                likedByMe,
                bookmarked,
                post.getCreatedAt()
        );
    }
}
