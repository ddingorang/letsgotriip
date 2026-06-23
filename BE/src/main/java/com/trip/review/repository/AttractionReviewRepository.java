package com.trip.review.repository;

import com.trip.review.entity.AttractionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttractionReviewRepository extends JpaRepository<AttractionReview, Long> {

    /** 시드 reset — 마커 사용자 소유 리뷰 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM AttractionReview r WHERE r.userId IN :userIds")
    int deleteByUserIdIn(@Param("userIds") List<Long> userIds);

    /** 특정 관광지 리뷰 최신순 */
    List<AttractionReview> findByContentIdOrderByCreatedAtDesc(String contentId);

    /** 내 리뷰 전체 최신순 (G11 — 내 리뷰 목록) */
    List<AttractionReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 소유자 검증용 — 리뷰 ID + 작성자 일치 조회 */
    Optional<AttractionReview> findByIdAndUserId(Long id, Long userId);

    /** 1인 1리뷰 중복 작성 여부 */
    boolean existsByUserIdAndContentId(Long userId, String contentId);

    /** 특정 관광지 평균 평점 (리뷰 없으면 null) */
    @Query("select avg(r.rating) from AttractionReview r where r.contentId = :contentId")
    Double findAverageRatingByContentId(@Param("contentId") String contentId);

    /**
     * contentId 목록에 대한 관광지명 매핑 (G11 — 내 리뷰 목록 표시용).
     * AttractionReview와 Attraction은 JPA 연관이 없으므로 contentId 동등 조인으로 묶는다.
     * 동일 contentId에 유형별 스냅샷이 여럿일 수 있어 contentId당 여러 행이 나올 수 있다(호출 측에서 병합).
     */
    @Query("""
            select a.contentId as contentId, a.title as title
            from com.trip.attraction.entity.Attraction a
            where a.contentId in :contentIds
            """)
    List<ContentTitleView> findAttractionTitlesByContentIds(@Param("contentIds") List<String> contentIds);

    /** contentId → 관광지명 투영 */
    interface ContentTitleView {
        String getContentId();
        String getTitle();
    }
}
