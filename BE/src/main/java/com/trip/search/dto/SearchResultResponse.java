package com.trip.search.dto;

import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/search 통합검색 결과 — 소스별 그룹.
 * attractions 는 TourAPI(AttractionService.search) 결과를 그대로 재사용한다.
 */
public record SearchResultResponse(
        List<AttractionItem> attractions,
        List<PostHit> posts,
        List<CompanionHit> companions,
        List<FestivalHit> festivals
) {

    /** community Post + HotPlace 통합 히트 (type: POST | HOTPLACE) */
    public record PostHit(
            Long id,
            String type,
            String title,
            String category
    ) {}

    /** companion 동행 게시글 히트 */
    public record CompanionHit(
            Long id,
            String title,
            String region,
            LocalDate travelDate
    ) {}

    /** festival 축제 히트 */
    public record FestivalHit(
            String contentId,
            String title,
            String address,
            String imageUrl,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
