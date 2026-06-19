package com.trip.favorite.dto;

import com.trip.favorite.entity.Favorite;
import com.trip.favorite.entity.FavoriteTargetType;

import java.time.LocalDateTime;

/**
 * 즐겨찾기 항목 응답.
 */
public record FavoriteResponse(
        Long id,
        FavoriteTargetType targetType,
        String targetId,
        LocalDateTime createdAt
) {
    public static FavoriteResponse of(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getTargetType(),
                favorite.getTargetId(),
                favorite.getCreatedAt()
        );
    }
}
