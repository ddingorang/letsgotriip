// Created: 2026-06-15 22:17:42
package com.trip.community.repository;

import com.trip.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostIdOrderByDisplayOrderAsc(Long postId);

    Optional<PostImage> findFirstByPostIdOrderByDisplayOrderAsc(Long postId);

    void deleteAllByPostId(Long postId);
}
