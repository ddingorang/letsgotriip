package com.trip.attraction.service;

import com.trip.attraction.dto.AttractionLikeResponse;
import com.trip.attraction.entity.Attraction;
import com.trip.attraction.entity.AttractionLike;
import com.trip.attraction.repository.AttractionLikeRepository;
import com.trip.attraction.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 관광지 좋아요(하트) 서비스 — 찜(Favorite)과 별개의 공개 인기 카운터.
 * 토글 시 AttractionLike 행을 생성/삭제하고 Attraction.likeCount 를 증감한다.
 * Attraction 행이 없으면 TourAPI 호출 없이 최소 스냅샷을 직접 생성한다(오프라인 안전).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttractionLikeService {

    private final AttractionRepository     attractionRepository;
    private final AttractionLikeRepository attractionLikeRepository;

    /**
     * 좋아요 토글.
     *  - 좋아요 행이 있으면: 삭제 + decLike()
     *  - 없으면: 생성 + incLike()
     * Attraction 행이 없으면 (contentId, contentType, title=name) 최소 행을 생성한다(TourAPI 미호출).
     */
    @Transactional
    public AttractionLikeResponse toggle(Long userId, String contentId, Integer contentType, String name) {
        Attraction attraction = getOrCreateMinimal(contentId, contentType, name);

        return attractionLikeRepository
                .findByUserIdAndContentIdAndContentType(userId, contentId, contentType)
                .map(existing -> {
                    // 이미 좋아요 → 취소
                    attractionLikeRepository.delete(existing);
                    attraction.decLike();
                    return new AttractionLikeResponse(false, attraction.getLikeCount());
                })
                .orElseGet(() -> {
                    // 좋아요 추가
                    try {
                        attractionLikeRepository.save(AttractionLike.builder()
                                .userId(userId)
                                .contentId(contentId)
                                .contentType(contentType)
                                .createdAt(LocalDateTime.now())
                                .build());
                        attraction.incLike();
                        return new AttractionLikeResponse(true, attraction.getLikeCount());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 토글 충돌 — 이미 좋아요 상태로 간주
                        log.info("좋아요 동시 insert 충돌 — 기존 상태 반환. userId={}, contentId={}", userId, contentId);
                        return new AttractionLikeResponse(true, attraction.getLikeCount());
                    }
                });
    }

    /**
     * 좋아요 상태 조회 — { liked, likeCount }.
     * likeCount 는 Attraction 행 기준(없으면 0), liked 는 좋아요 테이블 기준.
     * userId 가 null(비로그인)이면 liked=false.
     */
    public AttractionLikeResponse getState(Long userId, String contentId, Integer contentType) {
        int likeCount = attractionRepository
                .findByContentIdAndContentType(contentId, contentType)
                .map(Attraction::getLikeCount)
                .orElse(0);

        boolean liked = userId != null && attractionLikeRepository
                .existsByUserIdAndContentIdAndContentType(userId, contentId, contentType);

        return new AttractionLikeResponse(liked, likeCount);
    }

    /**
     * Attraction 행을 찾거나, 없으면 TourAPI 호출 없이 최소 스냅샷을 직접 생성한다.
     * (시드가 TourAPI 를 피하는 방식과 동일 — 오프라인 안전)
     */
    private Attraction getOrCreateMinimal(String contentId, Integer contentType, String name) {
        return attractionRepository.findByContentIdAndContentType(contentId, contentType)
                .orElseGet(() -> {
                    try {
                        return attractionRepository.save(Attraction.builder()
                                .contentId(contentId)
                                .contentType(contentType)
                                .title(name != null && !name.isBlank() ? name : contentId)
                                .fetchedAt(LocalDateTime.now())
                                .likeCount(0)
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 insert 충돌(uk_attr_content) — 이미 저장된 행 반환
                        return attractionRepository.findByContentIdAndContentType(contentId, contentType)
                                .orElseThrow(() -> e);
                    }
                });
    }
}
