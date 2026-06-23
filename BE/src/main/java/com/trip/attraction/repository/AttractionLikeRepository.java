package com.trip.attraction.repository;

import com.trip.attraction.entity.AttractionLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * AttractionLike JPA 리포지토리 — 사용자별 좋아요 상태 조회/토글.
 */
public interface AttractionLikeRepository extends JpaRepository<AttractionLike, Long> {

    Optional<AttractionLike> findByUserIdAndContentIdAndContentType(
            Long userId, String contentId, Integer contentType);

    boolean existsByUserIdAndContentIdAndContentType(
            Long userId, String contentId, Integer contentType);

    // 배치 상태 하이드레이션용(선택) — 여러 contentId의 좋아요 상태를 한 번에 조회
    List<AttractionLike> findByUserIdAndContentIdInAndContentType(
            Long userId, List<String> contentIds, Integer contentType);
}
