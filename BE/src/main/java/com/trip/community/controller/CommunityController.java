// Created: 2026-06-15 21:49:30
package com.trip.community.controller;

import com.trip.community.dto.*;
import com.trip.community.entity.enums.PostCategory;
import com.trip.community.service.CommunityService;
import com.trip.community.service.FileStorageService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final FileStorageService fileStorageService;

    // ─── 이미지 업로드 ────────────────────────────────────────

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart MultipartFile file
    ) {
        String imageUrl = fileStorageService.store(file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    // ─── 게시글 ───────────────────────────────────────────────

    @GetMapping("/posts")
    public ResponseEntity<CursorPageResponse<PostSummaryResponse>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(communityService.getPosts(category, cursor, size));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(communityService.getPost(postId, userId));
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PostCreateRequest request
    ) {
        PostResponse response = communityService.createPost(principal.userId(), request);
        return ResponseEntity.created(URI.create("/community/posts/" + response.id())).body(response);
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PostUpdateRequest request
    ) {
        return ResponseEntity.ok(communityService.updatePost(postId, principal.userId(), request));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        communityService.deletePost(postId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ─── 좋아요 ───────────────────────────────────────────────

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<Boolean> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean liked = communityService.toggleLike(postId, principal.userId());
        return ResponseEntity.ok(liked);
    }

    // ─── 댓글 ────────────────────────────────────────────────

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Long userId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(communityService.getComments(postId, userId, pageable));
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/likes")
    public ResponseEntity<Boolean> toggleCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(communityService.toggleCommentLike(commentId, principal.userId()));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.ok(communityService.createComment(postId, principal.userId(), request));
    }

    @PatchMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.ok(communityService.updateComment(commentId, principal.userId(), request));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        communityService.deleteComment(commentId, principal.userId());
        return ResponseEntity.noContent().build();
    }

}
