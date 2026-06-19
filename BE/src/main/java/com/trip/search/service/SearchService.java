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
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 통합검색 서비스.
 * - attraction: TourAPI 프록시(AttractionService.search) 재사용.
 * - post/hotplace/companion/festival: 각 도메인 리포지토리에서 제목/이름/주소 부분일치(대소문자 무시)를
 *   DB 쿼리로 직접 검색하고 {@code Pageable(0, RESULT_LIMIT)}로 상한한다(메모리 필터 제거).
 *
 * 오류 처리:
 * - type=attraction 단독 요청은 TourAPI 장애를 그대로 전파(502)한다.
 * - type=all 요청은 attraction(외부) 실패를 빈 결과 + {@code sourceErrors}(부분 실패 메타)로 흡수한다.
 * - 허용되지 않은 type은 400(_BAD_REQUEST)으로 거절한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    /** 그룹별 최종 반환 상한 */
    private static final int RESULT_LIMIT = 10;

    /** 허용 type 값(소문자) */
    private static final Set<String> ALLOWED_TYPES =
            Set.of("all", "attraction", "post", "companion", "festival");

    /** sourceErrors 에 기록되는 외부 소스 식별자 */
    private static final String SOURCE_ATTRACTION = "attraction";

    private final AttractionService attractionService;
    private final PostRepository postRepository;
    private final HotPlaceRepository hotPlaceRepository;
    private final CompanionPostRepository companionPostRepository;
    private final FestivalRepository festivalRepository;

    public SearchResultResponse search(String q, String type) {
        String keyword = q == null ? "" : q.trim();
        String scope = (type == null || type.isBlank()) ? "all" : type.trim().toLowerCase(Locale.ROOT);

        // 미지원 type은 빈 결과(200)가 아니라 400으로 거절
        if (!ALLOWED_TYPES.contains(scope)) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "지원하지 않는 검색 type 입니다: " + type);
        }

        if (keyword.isEmpty()) {
            return new SearchResultResponse(List.of(), List.of(), List.of(), List.of());
        }

        boolean all = "all".equals(scope);
        Pageable limit = PageRequest.of(0, RESULT_LIMIT);
        List<String> sourceErrors = new ArrayList<>();

        List<AttractionItem> attractions = (all || "attraction".equals(scope))
                ? searchAttractions(keyword, all, sourceErrors) : List.of();
        List<PostHit> posts = (all || "post".equals(scope))
                ? searchPosts(keyword, limit) : List.of();
        List<CompanionHit> companions = (all || "companion".equals(scope))
                ? searchCompanions(keyword, limit) : List.of();
        List<FestivalHit> festivals = (all || "festival".equals(scope))
                ? searchFestivals(keyword, limit) : List.of();

        return new SearchResultResponse(attractions, posts, companions, festivals, List.copyOf(sourceErrors));
    }

    // ── attraction: TourAPI 프록시 ────────────────────────────────────────────
    // 키워드 2자 미만은 빈 배열. 외부 장애는: all → sourceErrors 기록 후 빈 배열, 단독 → 전파.
    private List<AttractionItem> searchAttractions(String keyword, boolean all, List<String> sourceErrors) {
        if (keyword.length() < 2) {
            return List.of();
        }
        try {
            AttractionSearchRequestDto req = new AttractionSearchRequestDto(
                    null, null, null, keyword, 1, RESULT_LIMIT, null, null, null);
            List<AttractionItem> items = attractionService.search(req);
            return items == null ? List.of() : items;
        } catch (Exception e) {
            if (all) {
                // all 검색은 부분 실패를 메타로 노출하고 나머지 소스 결과는 보존
                log.warn("통합검색(all) attraction 조회 실패 — 부분 실패로 흡수. keyword={}, error={}",
                        keyword, e.getMessage());
                sourceErrors.add(SOURCE_ATTRACTION);
                return List.of();
            }
            // type=attraction 단독은 장애를 은폐하지 않고 전파
            log.warn("통합검색(attraction) 조회 실패 — 에러 전파. keyword={}, error={}", keyword, e.getMessage());
            throw e;
        }
    }

    // ── post + hotplace: DB 부분일치 검색 후 상한 병합 ────────────────────────
    private List<PostHit> searchPosts(String keyword, Pageable limit) {
        List<PostHit> postHits = postRepository
                .findAllByDeletedFalseAndTitleContainingIgnoreCaseOrderByIdDesc(keyword, limit)
                .stream()
                .map(this::toPostHit)
                .toList();

        List<PostHit> hotPlaceHits = hotPlaceRepository
                .searchByNameOrAddress(HotPlaceStatus.APPROVED, keyword, limit)
                .stream()
                .map(this::toHotPlaceHit)
                .toList();

        return java.util.stream.Stream.concat(postHits.stream(), hotPlaceHits.stream())
                .limit(RESULT_LIMIT)
                .toList();
    }

    private List<CompanionHit> searchCompanions(String keyword, Pageable limit) {
        return companionPostRepository.searchByTitleOrRegion(keyword, limit).stream()
                .map(this::toCompanionHit)
                .toList();
    }

    private List<FestivalHit> searchFestivals(String keyword, Pageable limit) {
        // 미경과(end_date >= today) 행사만, 제목 부분일치로 DB 검색 + 상한
        return festivalRepository
                .findByTitleContainingIgnoreCaseAndEndDateGreaterThanEqualOrderByStartDateAsc(
                        keyword, LocalDate.now(), limit)
                .stream()
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
}
