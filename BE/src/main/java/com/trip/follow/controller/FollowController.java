package com.trip.follow.controller;

import com.trip.follow.dto.FollowStatusResponse;
import com.trip.follow.dto.FollowToggleRequest;
import com.trip.follow.dto.FollowToggleResponse;
import com.trip.follow.dto.FollowUserResponse;
import com.trip.follow.service.FollowService;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * POST /api/follows                          — 팔로우 토글 {targetUserId}
 * GET  /api/follows/users/{userId}/follow-status — 요청자 기준 팔로우 상태 + 카운트
 * GET  /api/follows/me/following             — 내가 팔로우한 사용자 목록
 *
 * 인증 강제는 SecurityConfig(/api/follows/**)에서 처리.
 */
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /** 팔로우 토글 */
    @PostMapping
    public ResponseEntity<FollowToggleResponse> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FollowToggleRequest request
    ) {
        if (principal == null) {
            throw new GeneralException(ResponseCode._UNAUTHORIZED);
        }
        boolean following = followService.toggle(principal.userId(), request.targetUserId());
        long followerCount = followService.countFollowers(request.targetUserId());
        return ResponseEntity.ok(new FollowToggleResponse(request.targetUserId(), following, followerCount));
    }

    /** 요청자 기준 팔로우 상태 + 팔로워/팔로잉 수 */
    @GetMapping("/users/{userId}/follow-status")
    public ResponseEntity<FollowStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId
    ) {
        Long requesterId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(followService.getStatus(requesterId, userId));
    }

    /** 내가 팔로우한 사용자 목록 */
    @GetMapping("/me/following")
    public ResponseEntity<List<FollowUserResponse>> getMyFollowing(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new GeneralException(ResponseCode._UNAUTHORIZED);
        }
        return ResponseEntity.ok(followService.getFollowing(principal.userId()));
    }
}
