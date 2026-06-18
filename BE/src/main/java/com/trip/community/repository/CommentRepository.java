// Created: 2026-06-15 21:48:55
package com.trip.community.repository;

import com.trip.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByPostIdAndDeletedFalse(Long postId, Pageable pageable);

    int countByPostIdAndDeletedFalse(Long postId);

    Optional<Comment> findByIdAndDeletedFalse(Long id);
}
