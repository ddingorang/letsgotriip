package com.trip.review.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.review.dto.ReviewCreateRequest;
import com.trip.review.dto.ReviewListResponse;
import com.trip.review.dto.ReviewResponse;
import com.trip.review.dto.ReviewUpdateRequest;
import com.trip.review.entity.AttractionReview;
import com.trip.review.repository.AttractionReviewRepository;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final AttractionReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /** 관광지 리뷰 목록 + 평균/개수 (공개) */
    public ReviewListResponse listByContent(String contentId) {
        final List<AttractionReview> reviews = reviewRepository.findByContentIdOrderByCreatedAtDesc(contentId);

        final Map<Long, String> nicknames = userRepository.findAllById(
                        reviews.stream().map(AttractionReview::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        final List<ReviewResponse> responses = reviews.stream()
                .map(r -> ReviewResponse.from(r, nicknames.get(r.getUserId())))
                .toList();

        final Double avg = reviewRepository.findAverageRatingByContentId(contentId);
        return ReviewListResponse.of(avg == null ? 0.0 : avg, responses);
    }

    /** 리뷰 작성 — 1인 1리뷰. 이미 작성했으면 수정 유도 */
    @Transactional
    public ReviewResponse create(Long userId, String contentId, ReviewCreateRequest request) {
        if (reviewRepository.existsByUserIdAndContentId(userId, contentId)) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "이미 작성한 리뷰가 있습니다. 기존 리뷰를 수정해주세요.");
        }
        final AttractionReview review = reviewRepository.save(AttractionReview.builder()
                .userId(userId)
                .contentId(contentId)
                .rating(request.rating())
                .content(request.content())
                .build());
        return ReviewResponse.from(review, resolveNickname(userId));
    }

    /** 리뷰 수정 — 소유자 검증 */
    @Transactional
    public ReviewResponse update(Long userId, Long reviewId, ReviewUpdateRequest request) {
        final AttractionReview review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        review.update(request.rating(), request.content());
        return ReviewResponse.from(review, resolveNickname(userId));
    }

    /** 리뷰 삭제 — 소유자 검증 */
    @Transactional
    public void delete(Long userId, Long reviewId) {
        final AttractionReview review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        reviewRepository.delete(review);
    }

    private String resolveNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
    }
}
