// Created: 2026-06-15 23:26:08
package com.trip.community.dto;

import com.trip.community.entity.Post;
import com.trip.community.entity.enums.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        PostCategory category,
        List<String> imageUrls,
        boolean likedByMe,
        boolean bookmarked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse of(Post post, int commentCount, List<String> imageUrls, boolean likedByMe, boolean bookmarked) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getAuthor().getProfileImageUrl(),
                post.getViewCount(),
                post.getLikeCount(),
                commentCount,
                post.getCategory(),
                imageUrls,
                likedByMe,
                bookmarked,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
