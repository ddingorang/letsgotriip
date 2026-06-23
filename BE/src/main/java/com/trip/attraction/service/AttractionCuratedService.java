package com.trip.attraction.service;

import com.trip.attraction.dto.CuratedAttractionResponse;
import com.trip.attraction.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 큐레이션 조회 서비스 — DB 기반(태그 + 좋아요/최신순).
 * "큐레이션" = 배치로 태그가 부여된 행만 대상으로 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttractionCuratedService {

    private static final Set<String> VALID_TAGS = Set.of("food", "culture", "activity", "night");
    private static final int MAX_SIZE = 100;

    private final AttractionRepository attractionRepository;

    /**
     * 큐레이션 목록 조회.
     * @param tag  food|culture|activity|night, 또는 빈 값(=전체 큐레이션)
     * @param sort like(기본, 좋아요순) | latest(최신순)
     */
    public Page<CuratedAttractionResponse> getCurated(String tag, String sort, int page, int size) {
        int clampedSize = Math.min(Math.max(1, size), MAX_SIZE);
        Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize);

        boolean latest = "latest".equalsIgnoreCase(sort);
        String normalizedTag = tag == null ? "" : tag.trim().toLowerCase();

        // 태그가 주어졌으나 유효하지 않으면 전체로 폴백하지 않고 빈 결과를 반환한다(오인 노출 방지).
        if (!normalizedTag.isEmpty() && !VALID_TAGS.contains(normalizedTag)) {
            return Page.empty(pageable);
        }
        boolean hasTag = !normalizedTag.isEmpty();

        Page<com.trip.attraction.entity.Attraction> result;
        if (hasTag) {
            result = latest
                    ? attractionRepository.findByTagOrderByLatest(normalizedTag, pageable)
                    : attractionRepository.findByTagOrderByLike(normalizedTag, pageable);
        } else {
            // tag 없음(또는 유효하지 않은 태그) → 전체 큐레이션
            result = latest
                    ? attractionRepository.findCuratedOrderByLatest(pageable)
                    : attractionRepository.findCuratedOrderByLike(pageable);
        }

        return result.map(CuratedAttractionResponse::from);
    }
}
