package com.trip.review.repository;

import com.trip.review.entity.AttractionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttractionReviewRepository extends JpaRepository<AttractionReview, Long> {

    /** 특정 관광지 리뷰 최신순 */
    List<AttractionReview> findByContentIdOrderByCreatedAtDesc(String contentId);

    /** 소유자 검증용 — 리뷰 ID + 작성자 일치 조회 */
    Optional<AttractionReview> findByIdAndUserId(Long id, Long userId);

    /** 1인 1리뷰 중복 작성 여부 */
    boolean existsByUserIdAndContentId(Long userId, String contentId);

    /** 특정 관광지 평균 평점 (리뷰 없으면 null) */
    @Query("select avg(r.rating) from AttractionReview r where r.contentId = :contentId")
    Double findAverageRatingByContentId(@Param("contentId") String contentId);
}
