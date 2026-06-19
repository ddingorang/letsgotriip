package com.trip.review.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.review.dto.MyReviewListResponse;
import com.trip.review.dto.MyReviewResponse;
import com.trip.review.dto.ReviewCreateRequest;
import com.trip.review.dto.ReviewListResponse;
import com.trip.review.dto.ReviewResponse;
import com.trip.review.dto.ReviewUpdateRequest;
import com.trip.review.entity.AttractionReview;
import com.trip.review.repository.AttractionReviewRepository;
import com.trip.review.repository.AttractionReviewRepository.ContentTitleView;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final AttractionReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /** 내 리뷰 목록 (G11) — 인증 사용자의 전체 리뷰 최신순. 가능하면 관광지명 포함. */
    public MyReviewListResponse listByUser(Long userId) {
        final List<AttractionReview> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // contentId → 관광지명 매핑. 리뷰는 contentType을 보관하지 않으므로 contentId로만 조회한다.
        // 동일 contentId에 여러 스냅샷(유형별)이 있을 수 있으나 표시용 이름이므로 임의 1건을 사용한다.
        final List<String> contentIds = reviews.stream()
                .map(AttractionReview::getContentId)
                .distinct()
                .toList();
        final Map<String, String> names = contentIds.isEmpty()
                ? Map.of()
                : reviewRepository.findAttractionTitlesByContentIds(contentIds).stream()
                        .collect(Collectors.toMap(
                                ContentTitleView::getContentId,
                                ContentTitleView::getTitle,
                                (a, b) -> a
                        ));

        final List<MyReviewResponse> responses = reviews.stream()
                .map(r -> MyReviewResponse.from(r, names.get(r.getContentId())))
                .toList();

        return MyReviewListResponse.of(responses);
    }

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
        final AttractionReview review;
        try {
            // 동시 작성 레이스: exists 검사를 통과한 두 요청이 동시에 save하면
            // (user_id, content_id) unique 제약 충돌 → 400(이미 작성)으로 변환
            review = reviewRepository.saveAndFlush(AttractionReview.builder()
                    .userId(userId)
                    .contentId(contentId)
                    .rating(request.rating())
                    .content(request.content())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "이미 작성한 리뷰가 있습니다. 기존 리뷰를 수정해주세요.");
        }
        return ReviewResponse.from(review, resolveNickname(userId));
    }

    /** 리뷰 수정 — 소유자 + 경로 contentId 일치 검증 */
    @Transactional
    public ReviewResponse update(Long userId, String contentId, Long reviewId, ReviewUpdateRequest request) {
        final AttractionReview review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        verifyContent(review, contentId);
        review.update(request.rating(), request.content());
        return ReviewResponse.from(review, resolveNickname(userId));
    }

    /** 리뷰 삭제 — 소유자 + 경로 contentId 일치 검증 */
    @Transactional
    public void delete(Long userId, String contentId, Long reviewId) {
        final AttractionReview review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        verifyContent(review, contentId);
        reviewRepository.delete(review);
    }

    /**
     * 경로 contentId가 리뷰의 contentId와 일치하는지 검증.
     * 소유자라도 다른 관광지 경로로 자기 리뷰를 조작하는 것을 막는다.
     */
    private void verifyContent(AttractionReview review, String contentId) {
        if (!review.getContentId().equals(contentId)) {
            throw new GeneralException(ResponseCode._BAD_REQUEST);
        }
    }

    private String resolveNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
    }
}
