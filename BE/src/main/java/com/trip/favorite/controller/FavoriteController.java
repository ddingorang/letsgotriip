package com.trip.favorite.controller;

import com.trip.favorite.dto.FavoriteResponse;
import com.trip.favorite.dto.FavoriteToggleRequest;
import com.trip.favorite.dto.FavoriteToggleResponse;
import com.trip.favorite.entity.FavoriteTargetType;
import com.trip.favorite.service.FavoriteService;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 즐겨찾기/북마크 — 모두 인증 필요.
 * POST   /api/favorites              : 토글 {targetType, targetId}
 * GET    /api/favorites?type=        : 내 즐겨찾기 목록 (type 없으면 전체)
 * DELETE /api/favorites/{type}/{id}  : 명시적 해제
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<FavoriteToggleResponse> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FavoriteToggleRequest request
    ) {
        boolean favorited = favoriteService.toggle(
                principal.userId(), request.targetType(), request.targetId());
        return ResponseEntity.ok(new FavoriteToggleResponse(favorited));
    }

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "type", required = false) FavoriteTargetType type
    ) {
        return ResponseEntity.ok(favoriteService.list(principal.userId(), type));
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable FavoriteTargetType targetType,
            @PathVariable String targetId
    ) {
        favoriteService.remove(principal.userId(), targetType, targetId);
        return ResponseEntity.noContent().build();
    }
}
