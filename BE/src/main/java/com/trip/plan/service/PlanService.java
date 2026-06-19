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
import com.trip.recommend.dto.ItineraryDraft;
import com.trip.recommend.dto.RecommendRequestDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private static final int MAX_PLAN_DAYS = 14;
    private static final int MAX_PAGE_SIZE = 50;

    private final PlanRepository planRepository;
    private final AttractionService attractionService;
    private final EntityManager entityManager;

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
                .build();

        // 기간만큼 TripDay 자동 생성
        int totalDays = (int) (req.startDate().toEpochDay() - req.endDate().toEpochDay()) * -1 + 1;
        for (int i = 1; i <= totalDays; i++) {
            TripDay day = TripDay.builder().plan(plan).dayNo(i).build();
            plan.getDays().add(day);
        }

        planRepository.save(plan);
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
        return PlanDetailResponseDto.from(plan);
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

        plan.updateMeta(req.title(), newStart, newEnd, req.companions(), req.budget());
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
        String title = req.areaCode() + " 여행 (" + req.startDate() + " ~ " + req.endDate() + ")";

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

        return PlanDetailResponseDto.from(plan);
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────

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
