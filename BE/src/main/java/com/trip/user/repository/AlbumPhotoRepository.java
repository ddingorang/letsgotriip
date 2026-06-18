// Created: 2026-06-15 22:32:52
package com.trip.user.repository;

import com.trip.user.entity.AlbumPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, Long> {

    List<AlbumPhoto> findAllByAlbumIdOrderByDisplayOrderAsc(Long albumId);

    Optional<AlbumPhoto> findByIdAndAlbumId(Long id, Long albumId);

    int countByAlbumId(Long albumId);

    void deleteAllByAlbumId(Long albumId);

    int countByAlbumIdAndDisplayOrderGreaterThanEqual(Long albumId, int displayOrder);
}
