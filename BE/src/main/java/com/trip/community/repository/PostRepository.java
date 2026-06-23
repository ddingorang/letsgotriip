// Created: 2026-06-15 21:48:53
package com.trip.community.repository;

import com.trip.community.entity.Post;
import com.trip.community.entity.enums.PostCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 커서 기반 (무한 스크롤)
    List<Post> findAllByDeletedFalseOrderByIdDesc(Pageable pageable);

    List<Post> findAllByDeletedFalseAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    List<Post> findAllByDeletedFalseAndCategoryOrderByIdDesc(PostCategory category, Pageable pageable);

    List<Post> findAllByDeletedFalseAndCategoryAndIdLessThanOrderByIdDesc(PostCategory category, Long cursorId, Pageable pageable);

    // 통합검색: 제목 부분일치(대소문자 무시) + 미삭제, DB 상한
    List<Post> findAllByDeletedFalseAndTitleContainingIgnoreCaseOrderByIdDesc(String title, Pageable pageable);

    Optional<Post> findByIdAndDeletedFalse(Long id);

    /** 시드 reset — 마커 사용자가 작성한 모든 게시글(삭제 플래그 무관) */
    List<Post> findAllByAuthor_IdIn(List<Long> authorIds);
}
