// Created: 2026-06-15 22:54:24
package com.trip.community.dto;

import com.trip.community.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        String authorNickname,
        String authorProfileImageUrl,
        int likeCount,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse of(Comment comment, boolean likedByMe) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getNickname(),
                comment.getAuthor().getProfileImageUrl(),
                comment.getLikeCount(),
                likedByMe,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
