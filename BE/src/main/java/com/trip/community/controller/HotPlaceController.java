// Created: 2026-06-15 23:25:40
package com.trip.community.controller;

import com.trip.community.dto.*;
import com.trip.community.service.HotPlaceService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/community/hotplaces")
@RequiredArgsConstructor
public class HotPlaceController {

    private final HotPlaceService hotPlaceService;

    // ─── 조회 (비회원 가능) ───────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<HotPlaceSummaryResponse>> getHotPlaces(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(hotPlaceService.getApprovedHotPlaces(pageable));
    }

    @GetMapping("/{hotPlaceId}")
    public ResponseEntity<HotPlaceResponse> getHotPlace(
            @PathVariable Long hotPlaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(hotPlaceService.getHotPlace(hotPlaceId, userId));
    }

    // ─── 등록 신청 (회원) ─────────────────────────────────────

    @PostMapping
    public ResponseEntity<HotPlaceResponse> register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody HotPlaceCreateRequest request
    ) {
        HotPlaceResponse response = hotPlaceService.register(principal.userId(), request);
        return ResponseEntity.created(URI.create("/community/hotplaces/" + response.id())).body(response);
    }

    @PatchMapping("/{hotPlaceId}")
    public ResponseEntity<HotPlaceResponse> update(
            @PathVariable Long hotPlaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody HotPlaceUpdateRequest request
    ) {
        return ResponseEntity.ok(hotPlaceService.update(hotPlaceId, principal.userId(), request));
    }

    @DeleteMapping("/{hotPlaceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long hotPlaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        hotPlaceService.delete(hotPlaceId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ─── 관리자 ───────────────────────────────────────────────

    @GetMapping("/pending")
    public ResponseEntity<Page<HotPlaceSummaryResponse>> getPendingHotPlaces(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(hotPlaceService.getPendingHotPlaces(principal.userId(), pageable));
    }

    @PostMapping("/{hotPlaceId}/approve")
    public ResponseEntity<HotPlaceResponse> approve(
            @PathVariable Long hotPlaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(hotPlaceService.approve(hotPlaceId, principal.userId()));
    }

    @PostMapping("/{hotPlaceId}/reject")
    public ResponseEntity<HotPlaceResponse> reject(
            @PathVariable Long hotPlaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(hotPlaceService.reject(hotPlaceId, principal.userId()));
    }
}
