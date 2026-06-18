// Created: 2026-06-15 22:33:38
package com.trip.user.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.user.dto.*;
import com.trip.user.service.AlbumService;
import com.trip.user.service.AlbumStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users/me/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private final AlbumStorageService albumStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart MultipartFile file
    ) {
        String imageUrl = albumStorageService.store(file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @GetMapping
    public ResponseEntity<List<AlbumSummaryResponse>> getAlbums(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(albumService.getAlbums(principal.userId()));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> getAlbum(
            @PathVariable Long albumId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(albumService.getAlbum(albumId, principal.userId()));
    }

    @PostMapping
    public ResponseEntity<AlbumResponse> createAlbum(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AlbumCreateRequest request
    ) {
        AlbumResponse response = albumService.createAlbum(principal.userId(), request);
        return ResponseEntity.created(URI.create("/users/me/albums/" + response.id())).body(response);
    }

    @PatchMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> updateAlbum(
            @PathVariable Long albumId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AlbumUpdateRequest request
    ) {
        return ResponseEntity.ok(albumService.updateAlbum(albumId, principal.userId(), request));
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<Void> deleteAlbum(
            @PathVariable Long albumId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        albumService.deleteAlbum(albumId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
