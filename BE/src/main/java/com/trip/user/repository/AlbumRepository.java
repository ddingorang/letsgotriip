// Created: 2026-06-15 22:32:50
package com.trip.user.repository;

import com.trip.user.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Album> findByIdAndUserId(Long id, Long userId);
}
