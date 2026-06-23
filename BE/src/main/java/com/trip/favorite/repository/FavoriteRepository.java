package com.trip.favorite.repository;

import com.trip.favorite.entity.Favorite;
import com.trip.favorite.entity.FavoriteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /** 시드 reset — 마커 사용자 소유 즐겨찾기 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.userId IN :userIds")
    int deleteByUserIdIn(@Param("userIds") List<Long> userIds);

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
