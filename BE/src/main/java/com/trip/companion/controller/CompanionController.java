// Created: 2026-06-15 23:43:35
package com.trip.companion.controller;

import com.trip.companion.dto.*;
import com.trip.companion.service.CompanionService;
import com.trip.community.dto.CursorPageResponse;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/companion/posts")
@RequiredArgsConstructor
@Validated
public class CompanionController {

    private final CompanionService companionService;

    // ─── 내 참여 방 ───────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<List<MyCompanionRoomResponse>> getMyRooms(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // SecurityConfig에서 인증을 요구하지만, principal null인 비정상 호출도 401로 방어 (NPE→500 방지)
        if (principal == null) {
            throw new GeneralException(ResponseCode._UNAUTHORIZED);
        }
        return ResponseEntity.ok(companionService.getMyRooms(principal.userId()));
    }

    // ─── 게시글 ───────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CompanionPostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompanionPostCreateRequest request
    ) {
        CompanionPostResponse response = companionService.createPost(principal.userId(), request);
        return ResponseEntity.created(URI.create("/companion/posts/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<CompanionPostSummaryResponse>> getPosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(companionService.getPosts(cursor, size));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<CompanionPostResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(companionService.getPost(postId, userId));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<CompanionPostResponse> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompanionPostUpdateRequest request
    ) {
        return ResponseEntity.ok(companionService.updatePost(postId, principal.userId(), request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        companionService.deletePost(postId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{postId}/close")
    public ResponseEntity<Void> closePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        companionService.closePost(postId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ─── 동행 신청 ────────────────────────────────────────────

    @GetMapping("/{postId}/applications/me")
    public ResponseEntity<CompanionApplicationResponse> getMyApplication(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Optional<CompanionApplicationResponse> result = companionService.getMyApplication(postId, principal.userId());
        return result.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{postId}/applications")
    public ResponseEntity<CompanionApplicationResponse> apply(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CompanionApplicationRequest request
    ) {
        String message = request != null ? request.message() : null;
        CompanionApplicationResponse response = companionService.apply(postId, principal.userId(), message);
        return ResponseEntity.created(URI.create("/companion/posts/" + postId + "/applications/" + response.id()))
                .body(response);
    }

    @GetMapping("/{postId}/applications")
    public ResponseEntity<List<CompanionApplicationResponse>> getApplications(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(companionService.getApplications(postId, principal.userId()));
    }

    @PatchMapping("/{postId}/applications/{applicationId}/approve")
    public ResponseEntity<CompanionApplicationResponse> approve(
            @PathVariable Long postId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(companionService.approveApplication(postId, applicationId, principal.userId()));
    }

    @PatchMapping("/{postId}/applications/{applicationId}/reject")
    public ResponseEntity<CompanionApplicationResponse> reject(
            @PathVariable Long postId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(companionService.rejectApplication(postId, applicationId, principal.userId()));
    }

    @DeleteMapping("/{postId}/applications/{applicationId}")
    public ResponseEntity<Void> cancelApplication(
            @PathVariable Long postId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        companionService.cancelApplication(postId, applicationId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
