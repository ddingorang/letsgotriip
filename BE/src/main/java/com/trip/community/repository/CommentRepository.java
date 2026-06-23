// Created: 2026-06-15 21:48:55
package com.trip.community.repository;

import com.trip.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByPostIdAndDeletedFalse(Long postId, Pageable pageable);

    int countByPostIdAndDeletedFalse(Long postId);

    Optional<Comment> findByIdAndDeletedFalse(Long id);

    /** 시드 reset — 게시글에 달린 댓글 id 조회(댓글 좋아요 정리용) */
    @Query("SELECT c.id FROM Comment c WHERE c.post.id IN :postIds")
    List<Long> findIdsByPostIdIn(@Param("postIds") List<Long> postIds);

    /** 시드 reset — 게시글에 달린 댓글 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id IN :postIds")
    int deleteByPostIdIn(@Param("postIds") List<Long> postIds);
}
