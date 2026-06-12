package com.trip.recommend.dto;

import com.trip.recommend.entity.Recommendation;
import com.trip.recommend.entity.RecommendStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * 추천 결과 응답 DTO.
 * draft 필드는 resultJson을 파싱한 ItineraryDraft (FAILED 시 null).
 */
public record RecommendationResponseDto(
        Long id,
        RecommendStatus status,
        ItineraryDraft draft,
        LocalDateTime createdAt,
        Long savedPlanId
) {

    public static RecommendationResponseDto from(Recommendation rec, ObjectMapper objectMapper) {
        ItineraryDraft draft = null;
        if (rec.getResultJson() != null) {
            try {
                draft = objectMapper.readValue(rec.getResultJson(), ItineraryDraft.class);
            } catch (Exception ignored) {
                // 파싱 실패 시 null — 클라이언트는 status 기반으로 처리
            }
        }
        return new RecommendationResponseDto(
                rec.getId(),
                rec.getStatus(),
                draft,
                rec.getCreatedAt(),
                rec.getSavedPlanId()
        );
    }
}
