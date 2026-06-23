// Created: 2026-06-15 21:48:56
package com.trip.community.repository;

import com.trip.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /** 시드 reset — 주어진 게시글들의 좋아요 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id IN :postIds")
    int deleteByPostIdIn(@Param("postIds") List<Long> postIds);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    /**
     * 목록 화면의 likedByMe 매핑을 위해, 주어진 게시글 집합 중 사용자가 좋아요한 게시글 id 만 한 번에 조회한다(N+1 방지).
     */
    @Query("select pl.post.id from PostLike pl where pl.user.id = :userId and pl.post.id in :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
