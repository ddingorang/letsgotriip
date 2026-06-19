// Created: 2026-06-15 22:32:50
package com.trip.user.repository;

import com.trip.user.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Album> findByIdAndUserId(Long id, Long userId);

    /** 공유 토큰으로 앨범 조회 (G12 — 공개 조회) */
    Optional<Album> findByShareToken(String shareToken);
}
