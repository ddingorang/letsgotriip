package com.trip.plan.service;

import com.trip.attraction.entity.Attraction;
import com.trip.attraction.service.AttractionService;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.PlanHandler;
import com.trip.plan.dto.*;
import com.trip.plan.entity.CompanionsType;
import com.trip.plan.entity.OriginType;
import com.trip.plan.entity.TripDay;
import com.trip.plan.entity.TripPlace;
import com.trip.plan.entity.TripPlan;
import com.trip.plan.repository.PlanRepository;
import com.trip.rag.UserDataIndexer;
import com.trip.recommend.dto.ItineraryDraft;
import com.trip.recommend.dto.RecommendRequestDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan 도메인 서비스.
 *
 * version 증가 전략:
 * - plan 메타 수정: JPA @Version 자동 증가 (dirty-check)
 * - place 추가/삭제/교체: plan 엔티티 자체 필드는 변경되지 않으므로
 *   entityManager.lock(plan, LockModeType.OPTIMISTIC_FORCE_INCREMENT) 호출.
 *   이 방식은 트랜잭션 커밋 시 Hibernate가 version을 강제 증가시킨다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private static final int MAX_PLAN_DAYS = 14;
    private static final int MAX_PAGE_SIZE = 50;

    /** 지역코드 → 지역명 — AI 추천 계획 기본 제목 생성용("39 여행" 대신 "제주 여행"). */
    private static final java.util.Map<String, String> AREA_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("1", "서울"), java.util.Map.entry("2", "인천"),
            java.util.Map.entry("3", "대전"), java.util.Map.entry("4", "대구"),
            java.util.Map.entry("5", "광주"), java.util.Map.entry("6", "부산"),
            java.util.Map.entry("7", "울산"), java.util.Map.entry("8", "세종"),
            java.util.Map.entry("31", "경기"), java.util.Map.entry("32", "강원"),
            java.util.Map.entry("33", "충북"), java.util.Map.entry("34", "충남"),
            java.util.Map.entry("35", "경북"), java.util.Map.entry("36", "경남"),
            java.util.Map.entry("37", "전북"), java.util.Map.entry("38", "전남"),
            java.util.Map.entry("39", "제주"));

    private final PlanRepository planRepository;
    private final AttractionService attractionService;
    private final EntityManager entityManager;
    private final UserDataIndexer userDataIndexer;
    private final com.trip.plan.client.KakaoDirectionsClient kakaoDirectionsClient;
    // 동선(route-path) 캐시용 — 계획이 안 바뀌면(version 동일) 카카오 재호출 없이 반환
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // 생성
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PlanDetailResponseDto create(Long userId, PlanCreateRequestDto req) {
        validatePeriod(req.startDate(), req.endDate());

        TripPlan plan = TripPlan.builder()
                .userId(userId)
                .title(req.title())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .companions(req.companions())
                .budget(req.budget())
                .origin(req.origin())
                .imageUrl(req.imageUrl())
                .build();

        // 기간만큼 TripDay 자동 생성
        int totalDays = (int) (req.startDate().toEpochDay() - req.endDate().toEpochDay()) * -1 + 1;
        for (int i = 1; i <= totalDays; i++) {
            TripDay day = TripDay.builder().plan(plan).dayNo(i).build();
            plan.getDays().add(day);
        }

        planRepository.save(plan);

        indexPlanSafely(userId, plan.getId());

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 목록 조회
    // ─────────────────────────────────────────────────────────────

    public Page<PlanSummaryResponseDto> getMyPlans(Long userId, int page, int size) {
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize);
        return planRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(PlanSummaryResponseDto::from);
    }

    // ─────────────────────────────────────────────────────────────
    // 상세 조회
    // ─────────────────────────────────────────────────────────────

    public PlanDetailResponseDto getDetail(Long userId, Long planId) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);
        return toDetailDto(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 공유 — 토큰 발급 / 공개 조회
    // ─────────────────────────────────────────────────────────────

    /**
     * 공유 토큰 발급(소유자 전용). 이미 발급된 경우 기존 토큰을 재사용(idempotent).
     */
    @Transactional
    public PlanShareResponseDto createShare(Long userId, Long planId) {
        TripPlan plan = findPlan(planId);
        verifyOwner(plan, userId);
        if (plan.getShareToken() == null) {
            plan.markShared(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        return PlanShareResponseDto.of(plan.getShareToken());
    }

    /**
     * 공유 토큰으로 공개 조회(소유 검증 없음). 토큰이 없으면 PLAN_NOT_FOUND.
     */
    public PlanDetailResponseDto getShared(String token) {
        TripPlan plan = planRepository.findByShareToken(token)
                .orElseThrow(() -> new PlanHandler(ResponseCode.PLAN_NOT_FOUND));
        return toDetailDto(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 비교 — 두 계획 요약 통계
    // ─────────────────────────────────────────────────────────────

    /**
     * 두 계획 비교(둘 다 소유자여야 함, 아니면 PLAN_FORBIDDEN).
     */
    public PlanCompareResponseDto compare(Long userId, Long aId, Long bId) {
        TripPlan a = findPlanWithDays(aId);
        verifyOwner(a, userId);
        TripPlan b = findPlanWithDays(bId);
        verifyOwner(b, userId);
        return PlanCompareResponseDto.of(a, b);
    }

    /**
     * N개 계획 비교(모두 요청자 본인 소유여야 함, 아니면 PLAN_FORBIDDEN).
     * 중복 id는 제거하고 요청 순서를 유지한다. 유효 id가 2개 미만이면 PLAN_COMPARE_BAD_REQUEST.
     */
    public PlanCompareResponseDto.PlanCompareListResponseDto compareMany(Long userId, List<Long> ids) {
        // null 방어 + 중복 제거(요청 순서 유지)
        List<Long> distinctIds = (ids == null ? List.<Long>of() : ids).stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.size() < 2) {
            throw new PlanHandler(ResponseCode.PLAN_COMPARE_BAD_REQUEST);
        }
        List<TripPlan> plans = distinctIds.stream()
                .map(id -> {
                    TripPlan p = findPlanWithDays(id);
                    verifyOwner(p, userId);   // 남의 계획이 섞이면 즉시 거부
                    return p;
                })
                .toList();
        return PlanCompareResponseDto.PlanCompareListResponseDto.of(plans);
    }

    // ─────────────────────────────────────────────────────────────
    // 예산 — 카테고리 기반 추정(데모)
    // ─────────────────────────────────────────────────────────────

    /**
     * 카테고리(contentType)별 기본 단가로 일자별/전체 추정 비용 산출(소유자 전용).
     * 가격 필드가 없으므로 데모 추정치다.
     */
    public PlanBudgetResponseDto getBudget(Long userId, Long planId) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);

        List<PlanBudgetResponseDto.DayBudget> dayBudgets = new ArrayList<>();
        long total = 0;
        for (TripDay day : plan.getDays()) {
            long cost = day.getPlaces().stream()
                    .mapToLong(p -> estimatePlaceCost(p.getAttraction().getContentType()))
                    .sum();
            total += cost;
            dayBudgets.add(new PlanBudgetResponseDto.DayBudget(day.getDayNo(), cost));
        }

        Integer planned = plan.getBudget();
        Integer difference = planned != null ? (int) (planned - total) : null;

        return new PlanBudgetResponseDto(
                plan.getId(),
                dayBudgets,
                total,
                planned,
                difference,
                "카테고리 기반 추정치(데모)"
        );
    }

    /** TourAPI contentType별 기본 단가 추정. 12=관광지, 39=음식점, 32=숙박, 그 외=기타. */
    private long estimatePlaceCost(Integer contentType) {
        if (contentType == null) return 10000L;
        return switch (contentType) {
            case 12 -> 5000L;   // 관광지
            case 39 -> 15000L;  // 음식점
            case 32 -> 80000L;  // 숙박
            default -> 10000L;  // 기타
        };
    }

    /** 동선 리포트 — 좌표 기반 거리·소요시간 + 최근접 이웃 추천 순서 */
    public com.trip.plan.dto.RouteReportResponseDto getRouteReport(Long userId, Long planId) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);
        return com.trip.plan.dto.RouteReportResponseDto.from(plan);
    }

    /**
     * 일자별 자동차 도로 경로(카카오 모빌리티 길찾기) 조회(소유자 전용).
     * 좌표 2개 미만인 날은 경로 없음(빈 path). 키 미설정/호출 실패 시 enabled=false.
     */
    public com.trip.plan.dto.RoutePathResponseDto getRoutePath(Long userId, Long planId) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);

        // ── 캐시 적중 — 계획이 안 바뀌면(version 동일) 카카오 길찾기 재호출 없이 반환 ──
        // version은 place 추가/삭제/교체 등 모든 하위 변경에서 증가하므로, 편집 시 자동 무효화된다.
        final Long version = plan.getVersion();
        final String cacheKey = "trip:routepath:" + planId + ":v" + version;

        // L1: Redis(빠름, 휘발성)
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, com.trip.plan.dto.RoutePathResponseDto.class);
            }
        } catch (Exception e) {
            log.warn("route-path Redis 읽기 실패 — planId={}, err={}", planId, e.getMessage());
        }

        // L2: DB 영속 캐시(계획에 저장 — 재시작/만료에도 보존). version이 맞으면 사용.
        if (plan.getRoutePathJson() != null && version.equals(plan.getRoutePathVersion())) {
            try {
                com.trip.plan.dto.RoutePathResponseDto dbDto = objectMapper.readValue(
                        plan.getRoutePathJson(), com.trip.plan.dto.RoutePathResponseDto.class);
                // L1 재적재
                try { stringRedisTemplate.opsForValue().set(cacheKey, plan.getRoutePathJson(),
                        java.time.Duration.ofDays(7)); } catch (Exception ignore) { /* noop */ }
                return dbDto;
            } catch (Exception e) {
                log.warn("route-path DB 캐시 역직렬화 실패 — planId={}, err={}", planId, e.getMessage());
            }
        }

        boolean enabled = kakaoDirectionsClient.isEnabled();
        List<com.trip.plan.dto.RoutePathResponseDto.DayPath> days = new ArrayList<>();
        // 한 일자라도 직선 근사/조회실패가 섞이면 '근사 포함' — 카카오 일시장애로 만든 부정확한
        // 경로를 plan.version 캐시에 장기 고착시키지 않도록 캐시 전략을 바꾼다(R4).
        boolean anyApprox = false;

        for (TripDay day : plan.getDays()) {
            List<com.trip.plan.client.KakaoDirectionsClient.Point> points = new ArrayList<>();
            for (TripPlace p : day.getPlaces()) {
                Attraction a = p.getAttraction();
                if (a == null || a.getLatitude() == null || a.getLongitude() == null) continue;
                points.add(new com.trip.plan.client.KakaoDirectionsClient.Point(
                        a.getLatitude(), a.getLongitude()));
            }
            if (!enabled || points.size() < 2) {
                // 좌표 부족/키 미설정 — 경로 없음(빈 path). 이는 '근사'가 아니라 안정 상태.
                days.add(new com.trip.plan.dto.RoutePathResponseDto.DayPath(
                        day.getDayNo(), 0, 0, 0, 0, false, java.util.List.of()));
                continue;
            }
            // 1) 하루 전체를 한 번에 조회(성공 시 전부 실제 도로경로).
            com.trip.plan.client.KakaoDirectionsClient.RouteResult r = kakaoDirectionsClient.route(points);
            // 2) 실패(한 지점이라도 비도로면 전체 실패) 시 구간별로 재시도 — 되는 구간은 실제 도로,
            //    안 되는 구간만 직선으로 이어 항상 동선이 보이게 한다.
            if (r == null) r = kakaoDirectionsClient.routeStitched(points);
            if (r == null) {
                // 도로/직선 폴백 모두 실패(예: API 전면 장애) — 전송 경로 없음 + 일시 실패로 간주.
                anyApprox = true;
                days.add(new com.trip.plan.dto.RoutePathResponseDto.DayPath(
                        day.getDayNo(), 0, 0, 0, 0, true, java.util.List.of()));
            } else {
                if (r.fallback()) anyApprox = true;
                days.add(new com.trip.plan.dto.RoutePathResponseDto.DayPath(
                        day.getDayNo(), r.distanceMeters(), r.durationSeconds(),
                        r.taxiFare(), r.tollFare(), r.fallback(), r.path()));
            }
        }
        com.trip.plan.dto.RoutePathResponseDto dto =
                new com.trip.plan.dto.RoutePathResponseDto(plan.getId(), enabled, days);

        // ── 캐시 저장 ──────────────────────────────────────────────────────────────
        // 전부 실제 도로경로면 DB(영속) + Redis 7일. 일부라도 직선 근사/조회실패가 섞이면
        // DB 영속을 보류하고 Redis 단기(10분)만 — 카카오 회복 시 곧 재계산되도록(R4).
        if (enabled) {
            try {
                String json = objectMapper.writeValueAsString(dto);
                if (!anyApprox) {
                    try {
                        planRepository.updateRoutePathCache(planId, json, version);
                    } catch (Exception e) {
                        log.warn("route-path DB 저장 실패 — planId={}, err={}", planId, e.getMessage());
                    }
                    try {
                        stringRedisTemplate.opsForValue().set(cacheKey, json, java.time.Duration.ofDays(7));
                    } catch (Exception e) {
                        log.warn("route-path Redis 저장 실패 — planId={}, err={}", planId, e.getMessage());
                    }
                } else {
                    // 근사 포함 — DB 영속 보류, Redis 단기 캐시만(반복 조회 폭주는 막되 고착은 방지).
                    try {
                        stringRedisTemplate.opsForValue().set(cacheKey, json, java.time.Duration.ofMinutes(10));
                    } catch (Exception e) {
                        log.warn("route-path Redis 단기 저장 실패 — planId={}, err={}", planId, e.getMessage());
                    }
                    log.info("route-path 근사 포함 — 단기 캐시(10분)·DB 영속 보류. planId={}", planId);
                }
            } catch (Exception e) {
                log.warn("route-path 직렬화 실패 — planId={}, err={}", planId, e.getMessage());
            }
        }
        return dto;
    }

    // ─────────────────────────────────────────────────────────────
    // 메타 수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PlanDetailResponseDto update(Long userId, Long planId, PlanUpdateRequestDto req) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);
        verifyVersion(plan, req.expectedVersion());
        validatePeriod(req.startDate(), req.endDate());

        LocalDate newStart = req.startDate();
        LocalDate newEnd   = req.endDate();
        int newTotalDays   = (int) (newEnd.toEpochDay() - newStart.toEpochDay()) + 1;
        int currentMax     = plan.getDays().stream().mapToInt(TripDay::getDayNo).max().orElse(0);

        // 기간 축소 — 장소 있는 일자 제거 시 409
        if (newTotalDays < currentMax) {
            boolean hasPlaceInRemovedDays = plan.getDays().stream()
                    .filter(d -> d.getDayNo() > newTotalDays)
                    .anyMatch(d -> !d.getPlaces().isEmpty());
            if (hasPlaceInRemovedDays) {
                throw new PlanHandler(ResponseCode.PLAN_PERIOD_CONFLICT);
            }
            // 장소 없는 일자만 제거 → orphanRemoval=true가 처리
            plan.getDays().removeIf(d -> d.getDayNo() > newTotalDays);
        }

        // 기간 연장 — 빈 day 추가
        if (newTotalDays > currentMax) {
            for (int i = currentMax + 1; i <= newTotalDays; i++) {
                TripDay day = TripDay.builder().plan(plan).dayNo(i).build();
                plan.getDays().add(day);
            }
        }

        plan.updateMeta(req.title(), newStart, newEnd, req.companions(), req.budget(), req.imageUrl());

        indexPlanSafely(userId, plan.getId());

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 삭제
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long userId, Long planId) {
        TripPlan plan = findPlan(planId);
        verifyOwner(plan, userId);
        planRepository.delete(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 장소 추가
    // ─────────────────────────────────────────────────────────────

    // [정책] 추가/삭제는 append형 연산으로 버전 검증 생략(의도). 전체 교체(PUT)만 버전 요구.
    @Transactional
    public PlanDetailResponseDto addPlace(Long userId, Long planId, int dayNo, PlaceAddRequestDto req) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);

        TripDay day = findDay(plan, dayNo);

        // 중복 장소 확인 (같은 day, 같은 contentId+contentType의 attraction)
        Attraction attraction = attractionService.upsertSnapshot(req.contentId(), req.contentType());
        boolean duplicated = day.getPlaces().stream()
                .anyMatch(p -> p.getAttraction().getId().equals(attraction.getId()));
        if (duplicated) {
            throw new PlanHandler(ResponseCode.DUPLICATE_PLACE);
        }

        int nextSeq = day.getPlaces().stream()
                .mapToInt(TripPlace::getSeq)
                .max()
                .orElse(0) + 1;

        TripPlace place = TripPlace.builder()
                .day(day)
                .attraction(attraction)
                .seq(nextSeq)
                .visitTime(req.visitTime())
                .memo(req.memo())
                .build();
        day.getPlaces().add(place);

        // plan version 강제 증가 (place 변경이므로 plan 필드는 dirty하지 않음)
        entityManager.lock(plan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 장소 전체 교체
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PlanDetailResponseDto replacePlaces(Long userId, Long planId, int dayNo,
                                               PlacesReplaceRequestDto req) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);
        verifyVersion(plan, req.expectedVersion());

        TripDay day = findDay(plan, dayNo);

        // 1) attraction 참조를 먼저 해석 (placeId 항목은 삭제 전에 조회해야 함)
        List<Attraction> resolved = new ArrayList<>();
        for (PlaceItemDto item : req.places()) {
            Attraction attraction;
            if (item.contentId() != null && item.contentType() != null) {
                attraction = attractionService.upsertSnapshot(item.contentId(), item.contentType());
            } else if (item.placeId() != null) {
                // 소속 검증: null이거나 다른 plan 소속이면 PLAN_NOT_FOUND
                TripPlace ref = entityManager.find(TripPlace.class, item.placeId());
                if (ref == null || !ref.getDay().getPlan().getId().equals(planId)) {
                    throw new PlanHandler(ResponseCode.PLAN_NOT_FOUND);
                }
                attraction = ref.getAttraction();
            } else {
                throw new PlanHandler(ResponseCode._BAD_REQUEST);
            }
            resolved.add(attraction);
        }

        // 2) 기존 장소 전체 삭제 후 flush — Hibernate는 INSERT를 DELETE보다 먼저 내보내므로
        //    flush 없이는 (day_id, seq) 유니크 제약과 충돌한다
        day.getPlaces().clear();
        entityManager.flush();

        // 3) 새 장소 목록으로 교체
        List<TripPlace> newPlaces = new ArrayList<>();
        for (int i = 0; i < req.places().size(); i++) {
            PlaceItemDto item = req.places().get(i);
            newPlaces.add(TripPlace.builder()
                    .day(day)
                    .attraction(resolved.get(i))
                    .seq(item.seq())
                    .visitTime(item.visitTime())
                    .memo(item.memo())
                    .build());
        }
        day.getPlaces().addAll(newPlaces);

        entityManager.lock(plan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 장소 삭제
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PlanDetailResponseDto removePlace(Long userId, Long planId, int dayNo, Long placeId) {
        TripPlan plan = findPlanWithDays(planId);
        verifyOwner(plan, userId);

        TripDay day = findDay(plan, dayNo);

        boolean removed = day.getPlaces().removeIf(p -> p.getId().equals(placeId));
        if (!removed) {
            throw new PlanHandler(ResponseCode.PLAN_NOT_FOUND);
        }

        entityManager.lock(plan, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // AI 추천 초안 → 여행 계획 생성 (recommend 도메인에서 위임)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PlanDetailResponseDto createFromDraft(Long userId, ItineraryDraft draft, RecommendRequestDto req) {
        String areaName = AREA_NAMES.getOrDefault(req.areaCode(), req.areaCode());
        String title = (req.title() != null && !req.title().isBlank())
                ? req.title()
                : areaName + " 여행 (" + req.startDate() + " ~ " + req.endDate() + ")";

        CompanionsType companions = null;
        if (req.companions() != null && !req.companions().isBlank()) {
            try {
                companions = CompanionsType.valueOf(req.companions().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 값은 null 처리
            }
        }

        TripPlan plan = TripPlan.builder()
                .userId(userId)
                .title(title)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .companions(companions)
                .budget(req.budget())
                .origin(OriginType.AI)
                .build();

        int totalDays = req.totalDays();
        for (int i = 1; i <= totalDays; i++) {
            TripDay day = TripDay.builder().plan(plan).dayNo(i).build();
            plan.getDays().add(day);
        }

        planRepository.save(plan);

        // 초안의 각 일자 장소를 plan에 추가
        if (draft.days() != null) {
            for (ItineraryDraft.DayPlan dayPlan : draft.days()) {
                int dayNo = dayPlan.dayNo();
                if (dayNo < 1 || dayNo > totalDays) continue;

                TripDay day = plan.getDays().stream()
                        .filter(d -> d.getDayNo() == dayNo)
                        .findFirst()
                        .orElse(null);
                if (day == null) continue;

                int seq = 1;
                if (dayPlan.places() != null) {
                    for (ItineraryDraft.PlaceRecommendation p : dayPlan.places()) {
                        if (p.contentId() == null) continue;
                        try {
                            // draft(PlaceRecommendation)에는 유형 필드가 없으므로
                            // detailCommon2 응답의 contentTypeId로 실제 유형을 추론한다
                            // (응답에 유형이 없으면 AttractionService가 12=관광지로 폴백).
                            // 음식점(39)·축제(15) 등이 12로 오분류되던 문제 해소.
                            Attraction attraction = attractionService.upsertSnapshot(p.contentId());

                            LocalTime visitTime = null;
                            if (p.visitTime() != null && !p.visitTime().isBlank()) {
                                try {
                                    visitTime = LocalTime.parse(p.visitTime());
                                } catch (DateTimeParseException ignored) {
                                    // 형식 불일치 시 null
                                }
                            }

                            TripPlace place = TripPlace.builder()
                                    .day(day)
                                    .attraction(attraction)
                                    .seq(seq++)
                                    .visitTime(visitTime)
                                    .memo(p.reason())
                                    .build();
                            day.getPlaces().add(place);
                        } catch (Exception e) {
                            // 개별 장소 실패 시 스킵 (전체 저장 실패 방지)
                        }
                    }
                }
            }
        }

        indexPlanSafely(userId, plan.getId());

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────

    /** 상세 응답 변환 — getDetail/getShared 공통. */
    private PlanDetailResponseDto toDetailDto(TripPlan plan) {
        return PlanDetailResponseDto.from(plan);
    }

    /**
     * 플랜을 RAG 벡터스토어에 색인한다("내 지난 여행" 검색용).
     * 임베딩(외부) 호출 실패가 본 트랜잭션을 깨면 안 되므로 예외는 로그만 남기고 삼킨다.
     */
    private void indexPlanSafely(Long userId, Long planId) {
        try {
            userDataIndexer.indexPlan(userId, planId);
        } catch (Exception e) {
            log.warn("plan RAG 색인 실패 — userId={}, planId={}, error={}", userId, planId, e.getMessage());
        }
    }

    private TripPlan findPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new PlanHandler(ResponseCode.PLAN_NOT_FOUND));
    }

    private TripPlan findPlanWithDays(Long planId) {
        return planRepository.findByIdWithDays(planId)
                .orElseThrow(() -> new PlanHandler(ResponseCode.PLAN_NOT_FOUND));
    }

    private void verifyOwner(TripPlan plan, Long userId) {
        if (!plan.getUserId().equals(userId)) {
            throw new PlanHandler(ResponseCode.PLAN_FORBIDDEN);
        }
    }

    private void verifyVersion(TripPlan plan, Long expectedVersion) {
        if (!plan.getVersion().equals(expectedVersion)) {
            throw new PlanHandler(ResponseCode.PLAN_VERSION_CONFLICT);
        }
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end) || (end.toEpochDay() - start.toEpochDay()) >= MAX_PLAN_DAYS) {
            throw new PlanHandler(ResponseCode.INVALID_PLAN_PERIOD);
        }
    }

    private TripDay findDay(TripPlan plan, int dayNo) {
        return plan.getDays().stream()
                .filter(d -> d.getDayNo() == dayNo)
                .findFirst()
                .orElseThrow(() -> new PlanHandler(ResponseCode.PLAN_NOT_FOUND));
    }
}
