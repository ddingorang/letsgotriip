package com.trip.review.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.review.dto.ReviewCreateRequest;
import com.trip.review.dto.ReviewListResponse;
import com.trip.review.dto.ReviewResponse;
import com.trip.review.dto.ReviewUpdateRequest;
import com.trip.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * GET    /api/attractions/{contentId}/reviews            — 리뷰 목록 + 평균/개수 (공개)
 * POST   /api/attractions/{contentId}/reviews            — 리뷰 작성 (인증)
 * PATCH  /api/attractions/{contentId}/reviews/{reviewId} — 리뷰 수정 (소유자)
 * DELETE /api/attractions/{contentId}/reviews/{reviewId} — 리뷰 삭제 (소유자)
 */
@RestController
@RequestMapping("/api/attractions/{contentId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ReviewListResponse> getReviews(@PathVariable String contentId) {
        return ResponseEntity.ok(reviewService.listByContent(contentId));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable String contentId,
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reviewService.create(principal.userId(), contentId, request));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String contentId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reviewService.update(principal.userId(), reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String contentId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.delete(principal.userId(), reviewId);
        return ResponseEntity.ok().build();
    }
}
