// Created: 2026-06-15 22:33:32
package com.trip.user.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.dto.*;
import com.trip.user.entity.Album;
import com.trip.user.entity.AlbumPhoto;
import com.trip.user.entity.User;
import com.trip.user.repository.AlbumPhotoRepository;
import com.trip.user.repository.AlbumRepository;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final UserRepository userRepository;
    private final AlbumStorageService albumStorageService;

    public List<AlbumSummaryResponse> getAlbums(Long userId) {
        return albumRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(album -> {
                    int photoCount = albumPhotoRepository.countByAlbumId(album.getId());
                    String thumbnailUrl = albumPhotoRepository
                            .findAllByAlbumIdOrderByDisplayOrderAsc(album.getId())
                            .stream().findFirst()
                            .map(AlbumPhoto::getImageUrl).orElse(null);
                    return AlbumSummaryResponse.of(album, photoCount, thumbnailUrl);
                })
                .toList();
    }

    public AlbumResponse getAlbum(Long albumId, Long userId) {
        Album album = findAlbum(albumId, userId);
        List<AlbumPhotoResponse> photos = albumPhotoRepository
                .findAllByAlbumIdOrderByDisplayOrderAsc(albumId).stream()
                .map(AlbumPhotoResponse::from).toList();
        return AlbumResponse.of(album, photos);
    }

    @Transactional
    public AlbumResponse createAlbum(Long userId, AlbumCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));

        Album album = albumRepository.save(Album.builder()
                .user(user)
                .name(request.name())
                .build());

        List<AlbumPhotoResponse> photos = savePhotos(album, request.imageUrls());
        return AlbumResponse.of(album, photos);
    }

    @Transactional
    public AlbumResponse updateAlbum(Long albumId, Long userId, AlbumUpdateRequest request) {
        Album album = findAlbum(albumId, userId);

        album.rename(request.name());

        // 사진 제거
        if (request.removePhotoIds() != null) {
            for (Long photoId : request.removePhotoIds()) {
                albumPhotoRepository.findByIdAndAlbumId(photoId, albumId).ifPresent(photo -> {
                    albumStorageService.delete(photo.getImageUrl());
                    albumPhotoRepository.delete(photo);
                });
            }
        }

        // 사진 추가 (기존 마지막 순서 이후로 추가)
        if (request.addImageUrls() != null && !request.addImageUrls().isEmpty()) {
            int nextOrder = albumPhotoRepository.countByAlbumId(albumId);
            for (int i = 0; i < request.addImageUrls().size(); i++) {
                albumPhotoRepository.save(AlbumPhoto.builder()
                        .album(album)
                        .imageUrl(request.addImageUrls().get(i))
                        .displayOrder(nextOrder + i)
                        .build());
            }
        }

        List<AlbumPhotoResponse> photos = albumPhotoRepository
                .findAllByAlbumIdOrderByDisplayOrderAsc(albumId).stream()
                .map(AlbumPhotoResponse::from).toList();
        return AlbumResponse.of(album, photos);
    }

    /**
     * 앨범 공유 토큰 발급(소유자 전용). 이미 발급된 경우 기존 토큰을 재사용(idempotent). (G12)
     */
    @Transactional
    public AlbumShareResponse share(Long albumId, Long userId) {
        Album album = findAlbum(albumId, userId);
        if (album.getShareToken() == null) {
            album.markShared(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        return AlbumShareResponse.of(album.getShareToken());
    }

    /**
     * 공유 토큰으로 공개 조회(소유 검증 없음). 토큰이 없으면 ALBUM_NOT_FOUND. (G12)
     */
    public AlbumResponse getShared(String token) {
        Album album = albumRepository.findByShareToken(token)
                .orElseThrow(() -> new GeneralException(ResponseCode.ALBUM_NOT_FOUND));
        List<AlbumPhotoResponse> photos = albumPhotoRepository
                .findAllByAlbumIdOrderByDisplayOrderAsc(album.getId()).stream()
                .map(AlbumPhotoResponse::from).toList();
        return AlbumResponse.of(album, photos);
    }

    @Transactional
    public void deleteAlbum(Long albumId, Long userId) {
        Album album = findAlbum(albumId, userId);

        albumPhotoRepository.findAllByAlbumIdOrderByDisplayOrderAsc(albumId)
                .forEach(photo -> albumStorageService.delete(photo.getImageUrl()));
        albumPhotoRepository.deleteAllByAlbumId(albumId);
        albumRepository.delete(album);
    }

    private List<AlbumPhotoResponse> savePhotos(Album album, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return Collections.emptyList();

        return java.util.stream.IntStream.range(0, imageUrls.size())
                .mapToObj(i -> albumPhotoRepository.save(AlbumPhoto.builder()
                        .album(album)
                        .imageUrl(imageUrls.get(i))
                        .displayOrder(i)
                        .build()))
                .map(AlbumPhotoResponse::from)
                .toList();
    }

    private Album findAlbum(Long albumId, Long userId) {
        return albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.ALBUM_NOT_FOUND));
    }
}
