package com.trip.review.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.review.dto.MyReviewListResponse;
import com.trip.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/reviews/me — 인증 사용자의 전체 리뷰 목록 (G11)
 *
 * ReviewController는 클래스 매핑이 /api/attractions/{contentId}/reviews 라
 * 내 리뷰 엔드포인트를 넣을 수 없어 별도 컨트롤러로 분리한다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class MyReviewController {

    private final ReviewService reviewService;

    @GetMapping("/me")
    public ResponseEntity<MyReviewListResponse> getMyReviews(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reviewService.listByUser(principal.userId()));
    }
}
