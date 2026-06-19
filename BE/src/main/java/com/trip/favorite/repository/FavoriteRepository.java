package com.trip.favorite.repository;

import com.trip.favorite.entity.Favorite;
import com.trip.favorite.entity.FavoriteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndTargetTypeAndTargetId(
            Long userId, FavoriteTargetType targetType, String targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(
            Long userId, FavoriteTargetType targetType, String targetId);

    /** 특정 종류만 — 최신순 */
    List<Favorite> findByUserIdAndTargetTypeOrderByCreatedAtDesc(
            Long userId, FavoriteTargetType targetType);

    /** 전체 — 최신순 */
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
