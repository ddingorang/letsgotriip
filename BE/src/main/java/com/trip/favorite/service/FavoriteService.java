package com.trip.favorite.service;

import com.trip.favorite.dto.FavoriteResponse;
import com.trip.favorite.entity.Favorite;
import com.trip.favorite.entity.FavoriteTargetType;
import com.trip.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    /**
     * 즐겨찾기 토글 — 이미 있으면 해제(false), 없으면 추가(true).
     */
    @Transactional
    public boolean toggle(Long userId, FavoriteTargetType targetType, String targetId) {
        return favoriteRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    try {
                        // 동시 추가 레이스: 두 요청이 동시에 부재를 확인하고 save하면
                        // (user_id, target_type, target_id) unique 충돌이 500으로 새는 것을 방지.
                        // 충돌은 곧 "이미 찜됨"을 의미하므로 멱등하게 favorited=true 반환.
                        favoriteRepository.saveAndFlush(Favorite.builder()
                                .userId(userId)
                                .targetType(targetType)
                                .targetId(targetId)
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        return true;
                    }
                    return true;
                });
    }

    /**
     * 명시적 해제 — 존재 여부와 무관하게 멱등 삭제.
     */
    @Transactional
    public void remove(Long userId, FavoriteTargetType targetType, String targetId) {
        favoriteRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .ifPresent(favoriteRepository::delete);
    }

    /**
     * 내 즐겨찾기 목록 — type 지정 시 해당 종류만, 없으면 전체.
     */
    public List<FavoriteResponse> list(Long userId, FavoriteTargetType targetType) {
        List<Favorite> favorites = (targetType == null)
                ? favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : favoriteRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, targetType);
        return favorites.stream()
                .map(FavoriteResponse::of)
                .toList();
    }

    /**
     * 특정 대상이 내 즐겨찾기인지 여부.
     */
    public boolean isFavorited(Long userId, FavoriteTargetType targetType, String targetId) {
        return favoriteRepository
                .existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }
}
