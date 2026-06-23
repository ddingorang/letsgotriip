// Created: 2026-06-15 22:54:07
package com.trip.community.repository;

import com.trip.community.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    /** 시드 reset — 주어진 댓글들의 좋아요 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id IN :commentIds")
    int deleteByCommentIdIn(@Param("commentIds") List<Long> commentIds);
}
