package com.trip.community.service;

import com.trip.community.dto.HotPlaceLikeResponse;
import com.trip.community.entity.HotPlace;
import com.trip.community.entity.HotPlaceLike;
import com.trip.community.repository.HotPlaceLikeRepository;
import com.trip.community.repository.HotPlaceRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 핫플 좋아요(하트) 서비스 — 찜(Favorite)과 별개의 공개 인기 카운터.
 * 토글 시 HotPlaceLike 행을 생성/삭제하고 HotPlace.likeCount 를 증감한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotPlaceLikeService {

    private final HotPlaceRepository hotPlaceRepository;
    private final HotPlaceLikeRepository hotPlaceLikeRepository;

    @Transactional
    public HotPlaceLikeResponse toggle(Long userId, Long hotPlaceId) {
        HotPlace hp = hotPlaceRepository.findById(hotPlaceId)
                .orElseThrow(() -> new GeneralException(ResponseCode.HOTPLACE_NOT_FOUND));

        return hotPlaceLikeRepository.findByUserIdAndHotPlaceId(userId, hotPlaceId)
                .map(existing -> {
                    hotPlaceLikeRepository.delete(existing);
                    hp.decLike();
                    return new HotPlaceLikeResponse(false, hp.getLikeCount());
                })
                .orElseGet(() -> {
                    try {
                        hotPlaceLikeRepository.save(HotPlaceLike.builder()
                                .userId(userId)
                                .hotPlaceId(hotPlaceId)
                                .createdAt(LocalDateTime.now())
                                .build());
                        hp.incLike();
                        return new HotPlaceLikeResponse(true, hp.getLikeCount());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 토글 충돌 — 이미 좋아요 상태로 간주
                        return new HotPlaceLikeResponse(true, hp.getLikeCount());
                    }
                });
    }

    public HotPlaceLikeResponse getState(Long userId, Long hotPlaceId) {
        int likeCount = hotPlaceRepository.findById(hotPlaceId).map(HotPlace::getLikeCount).orElse(0);
        boolean liked = userId != null && hotPlaceLikeRepository.existsByUserIdAndHotPlaceId(userId, hotPlaceId);
        return new HotPlaceLikeResponse(liked, likeCount);
    }
}
