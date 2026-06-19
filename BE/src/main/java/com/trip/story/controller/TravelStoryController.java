package com.trip.story.controller;

import com.trip.story.dto.TravelStoryCreateRequest;
import com.trip.story.dto.TravelStoryResponse;
import com.trip.story.dto.TravelStoryUpdateRequest;
import com.trip.story.service.TravelStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import com.trip.global.security.UserPrincipal;

/**
 * 여행 전후 스토리 API — 전부 인증 필요.
 *
 * GET    /api/stories       — 내 스토리 목록
 * GET    /api/stories/{id}  — 상세 (소유자만)
 * POST   /api/stories       — 생성
 * PATCH  /api/stories/{id}  — 부분 수정 (소유자만)
 * DELETE /api/stories/{id}  — 삭제 (소유자만)
 */
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class TravelStoryController {

    private final TravelStoryService travelStoryService;

    @GetMapping
    public ResponseEntity<List<TravelStoryResponse>> getStories(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(travelStoryService.listMy(principal.userId()));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<TravelStoryResponse> getStory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long storyId
    ) {
        return ResponseEntity.ok(travelStoryService.get(principal.userId(), storyId));
    }

    @PostMapping
    public ResponseEntity<TravelStoryResponse> createStory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TravelStoryCreateRequest request
    ) {
        TravelStoryResponse response = travelStoryService.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/stories/" + response.id())).body(response);
    }

    @PatchMapping("/{storyId}")
    public ResponseEntity<TravelStoryResponse> updateStory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long storyId,
            @Valid @RequestBody TravelStoryUpdateRequest request
    ) {
        return ResponseEntity.ok(travelStoryService.update(principal.userId(), storyId, request));
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long storyId
    ) {
        travelStoryService.delete(principal.userId(), storyId);
        return ResponseEntity.noContent().build();
    }
}
