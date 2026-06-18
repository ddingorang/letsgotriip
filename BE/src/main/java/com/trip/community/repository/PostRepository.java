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

    Optional<Post> findByIdAndDeletedFalse(Long id);
}
