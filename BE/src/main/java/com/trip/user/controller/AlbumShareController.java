// Created: 2026-06-20
package com.trip.user.controller;

import com.trip.user.dto.AlbumResponse;
import com.trip.user.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/albums/shared/{token} — 공유 토큰으로 앨범+사진 공개 조회 (비인증) (G12)
 *
 * 소유자용 앨범 API는 /users/me/albums 클래스 매핑이라
 * 공개 조회 엔드포인트를 넣을 수 없어 별도 컨트롤러로 분리한다.
 * (SecurityConfig에서 /api/albums/shared/** permitAll — 다른 스트림이 배선)
 */
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumShareController {

    private final AlbumService albumService;

    @GetMapping("/shared/{token}")
    public ResponseEntity<AlbumResponse> getSharedAlbum(@PathVariable String token) {
        return ResponseEntity.ok(albumService.getShared(token));
    }
}
