package com.trip.favorite.dto;

import com.trip.favorite.entity.FavoriteTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 즐겨찾기 토글/삭제 요청 바디.
 * targetId는 attraction contentId(문자열) 또는 post/hotplace id(Long을 문자열로).
 */
public record FavoriteToggleRequest(
        @NotNull FavoriteTargetType targetType,
        @NotBlank String targetId
) {
}
