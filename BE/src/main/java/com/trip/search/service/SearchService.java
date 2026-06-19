package com.trip.search.service;

import com.trip.attraction.dto.AttractionSearchRequestDto;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.service.AttractionService;
import com.trip.community.entity.HotPlace;
import com.trip.community.entity.Post;
import com.trip.community.entity.enums.HotPlaceStatus;
import com.trip.community.repository.HotPlaceRepository;
import com.trip.community.repository.PostRepository;
import com.trip.companion.entity.CompanionPost;
import com.trip.companion.repository.CompanionPostRepository;
import com.trip.festival.entity.Festival;
import com.trip.festival.repository.FestivalRepository;
import com.trip.search.dto.SearchResultResponse;
import com.trip.search.dto.SearchResultResponse.CompanionHit;
import com.trip.search.dto.SearchResultResponse.FestivalHit;
import com.trip.search.dto.SearchResultResponse.PostHit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 통합검색 서비스.
 * - attraction: TourAPI 프록시(AttractionService.search) 재사용 — 외부 실패는 빈 배열로 graceful.
 * - post/hotplace/companion/festival: 각 도메인 리포지토리의 기존 메서드로 상한 로드 후 제목/이름 메모리 필터.
 *   (타 도메인 리포지토리에 검색 메서드를 추가하지 않기 위한 의도적 선택)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    /** 그룹별 최종 반환 상한 */
    private static final int RESULT_LIMIT = 10;
    /** 메모리 필터를 위해 리포지토리에서 미리 로드하는 행 상한 (필터 전 모집단) */
    private static final int SCAN_LIMIT = 200;

    private final AttractionService attractionService;
    private final PostRepository postRepository;
    private final HotPlaceRepository hotPlaceRepository;
    private final CompanionPostRepository companionPostRepository;
    private final FestivalRepository festivalRepository;

    public SearchResultResponse search(String q, String type) {
        String keyword = q == null ? "" : q.trim();
        String scope = (type == null || type.isBlank()) ? "all" : type.trim().toLowerCase(Locale.ROOT);

        if (keyword.isEmpty()) {
            return new SearchResultResponse(List.of(), List.of(), List.of(), List.of());
        }

        boolean all = "all".equals(scope);
        String lower = keyword.toLowerCase(Locale.ROOT);

        List<AttractionItem> attractions = (all || "attraction".equals(scope))
                ? searchAttractions(keyword) : List.of();
        List<PostHit> posts = (all || "post".equals(scope))
                ? searchPosts(lower) : List.of();
        List<CompanionHit> companions = (all || "companion".equals(scope))
                ? searchCompanions(lower) : List.of();
        List<FestivalHit> festivals = (all || "festival".equals(scope))
                ? searchFestivals(lower) : List.of();

        return new SearchResultResponse(attractions, posts, companions, festivals);
    }

    // ── attraction: TourAPI 프록시. 키워드 2자 미만/외부 실패는 빈 배열 ───────────
    private List<AttractionItem> searchAttractions(String keyword) {
        if (keyword.length() < 2) {
            return List.of();
        }
        try {
            AttractionSearchRequestDto req = new AttractionSearchRequestDto(
                    null, null, null, keyword, 1, RESULT_LIMIT, null, null, null);
            List<AttractionItem> items = attractionService.search(req);
            return items == null ? List.of() : items;
        } catch (Exception e) {
            log.warn("통합검색 attraction 조회 실패 — 빈 배열 반환. keyword={}, error={}", keyword, e.getMessage());
            return List.of();
        }
    }

    // ── post + hotplace: 상한 로드 후 제목/이름 메모리 필터 ──────────────────────
    private List<PostHit> searchPosts(String lower) {
        Pageable scan = PageRequest.of(0, SCAN_LIMIT);

        List<PostHit> postHits = postRepository.findAllByDeletedFalseOrderByIdDesc(scan).stream()
                .filter(p -> contains(p.getTitle(), lower))
                .map(this::toPostHit)
                .limit(RESULT_LIMIT)
                .toList();

        List<PostHit> hotPlaceHits = hotPlaceRepository
                .findAllByStatusOrderByCreatedAtDesc(HotPlaceStatus.APPROVED, scan)
                .stream()
                .filter(h -> contains(h.getName(), lower) || contains(h.getAddress(), lower))
                .map(this::toHotPlaceHit)
                .limit(RESULT_LIMIT)
                .toList();

        return java.util.stream.Stream.concat(postHits.stream(), hotPlaceHits.stream())
                .limit(RESULT_LIMIT)
                .toList();
    }

    private List<CompanionHit> searchCompanions(String lower) {
        Pageable scan = PageRequest.of(0, SCAN_LIMIT);
        return companionPostRepository.findAllByDeletedFalseOrderByIdDesc(scan).stream()
                .filter(c -> contains(c.getTitle(), lower) || contains(c.getRegion(), lower))
                .map(this::toCompanionHit)
                .limit(RESULT_LIMIT)
                .toList();
    }

    private List<FestivalHit> searchFestivals(String lower) {
        // 종료(end_date < today) 행사는 제외하는 기존 메서드 활용 후 제목 메모리 필터
        List<Festival> active = festivalRepository.findByEndDateGreaterThanEqual(LocalDate.now());
        return active.stream()
                .filter(f -> contains(f.getTitle(), lower))
                .limit(RESULT_LIMIT)
                .map(this::toFestivalHit)
                .toList();
    }

    // ── 매핑 ────────────────────────────────────────────────────────────────
    private PostHit toPostHit(Post p) {
        return new PostHit(p.getId(), "POST", p.getTitle(),
                p.getCategory() != null ? p.getCategory().name() : null);
    }

    private PostHit toHotPlaceHit(HotPlace h) {
        return new PostHit(h.getId(), "HOTPLACE", h.getName(),
                h.getCategory() != null ? h.getCategory().name() : null);
    }

    private CompanionHit toCompanionHit(CompanionPost c) {
        return new CompanionHit(c.getId(), c.getTitle(), c.getRegion(), c.getTravelDate());
    }

    private FestivalHit toFestivalHit(Festival f) {
        return new FestivalHit(f.getContentId(), f.getTitle(), f.getAddress(),
                f.getImageUrl(), f.getStartDate(), f.getEndDate());
    }

    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lower);
    }
}
