// Created: 2026-06-15 23:25:05
package com.trip.community.dto;

import com.trip.community.entity.enums.HotPlaceCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HotPlaceCreateRequest(
        String name,
        String address,
        // 위도/경도는 지도 핀 지정으로 필수. 위경도 유효 범위를 벗어난 값은 거부한다.
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull HotPlaceCategory category,
        String description,
        List<String> imageUrls
) {}
