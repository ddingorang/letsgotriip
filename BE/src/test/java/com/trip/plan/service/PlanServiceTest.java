package com.trip.plan.service;

import com.trip.attraction.entity.Attraction;
import com.trip.attraction.service.AttractionService;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.PlanHandler;
import com.trip.plan.dto.PlaceAddRequestDto;
import com.trip.plan.dto.PlanCreateRequestDto;
import com.trip.plan.dto.PlanDetailResponseDto;
import com.trip.plan.dto.PlanUpdateRequestDto;
import com.trip.plan.entity.TripDay;
import com.trip.plan.entity.TripPlan;
import com.trip.plan.repository.PlanRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private AttractionService attractionService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PlanService planService;

    private static final Long OWNER_ID      = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PLAN_ID       = 10L;

    // ─────────────────────────────────────────────────────────────
    // Helpers — reflection으로 엔티티 필드 주입 (테스트용)
    // ─────────────────────────────────────────────────────────────

    private TripPlan makePlan(Long userId, LocalDate start, LocalDate end, Long version) {
        TripPlan plan = TripPlan.builder()
                .userId(userId)
                .title("테스트 여행")
                .startDate(start)
                .endDate(end)
                .build();
        setField(plan, "id", PLAN_ID);
        setField(plan, "version", version);
        setField(plan, "createdAt", LocalDateTime.now());
        setField(plan, "updatedAt", LocalDateTime.now());
        return plan;
    }

    private TripDay makeDay(TripPlan plan, int dayNo) {
        TripDay day = TripDay.builder().plan(plan).dayNo(dayNo).build();
        setField(day, "id", (long) dayNo);
        setField(day, "places", new ArrayList<>());
        return day;
    }

    private Attraction makeAttraction(Long id, String contentId, Integer contentType) {
        Attraction attr = Attraction.builder()
                .contentId(contentId)
                .contentType(contentType)
                .title("테스트 관광지")
                .fetchedAt(LocalDateTime.now())
                .build();
        setField(attr, "id", id);
        return attr;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = findField(target.getClass(), name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("테스트 픽스처 설정 실패: " + name, e);
        }
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        while (clazz != null) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { clazz = clazz.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    // ─────────────────────────────────────────────────────────────
    // 1. 정상 생성 — day 수 검증
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: 3일 여행 생성 시 TripDay 3개 자동 생성")
    void create_threeDayTrip_generatesThreeDays() {
        // given
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end   = LocalDate.of(2026, 7, 3);
        PlanCreateRequestDto req = new PlanCreateRequestDto(
                "3일 여행", start, end, null, null, null, null, null);

        given(planRepository.save(any(TripPlan.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        PlanDetailResponseDto result = planService.create(OWNER_ID, req);

        // then — 3일 → dayNo 1, 2, 3
        assertThat(result.days()).hasSize(3);
        assertThat(result.days().get(0).dayNo()).isEqualTo(1);
        assertThat(result.days().get(2).dayNo()).isEqualTo(3);
        verify(planRepository).save(any(TripPlan.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 소유자 위반 → 403
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDetail: 소유자 아닌 userId → PLAN_FORBIDDEN 403")
    void getDetail_nonOwner_throws403() {
        // given
        TripPlan plan = makePlan(OWNER_ID, LocalDate.now(), LocalDate.now().plusDays(1), 0L);
        given(planRepository.findByIdWithDays(PLAN_ID)).willReturn(Optional.of(plan));

        // when / then
        assertThatThrownBy(() -> planService.getDetail(OTHER_USER_ID, PLAN_ID))
                .isInstanceOf(PlanHandler.class)
                .satisfies(ex -> assertThat(((PlanHandler) ex).getErrorCode())
                        .isEqualTo(ResponseCode.PLAN_FORBIDDEN));
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 기간 축소 — 장소 있는 일자 → 409
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: 장소 있는 일자가 기간 축소로 제거되면 PLAN_PERIOD_CONFLICT 409")
    void update_shrinkPeriod_withPlaces_throwsConflict() {
        // given — 3일 플랜, day 3에 장소 있음
        LocalDate origStart = LocalDate.of(2026, 7, 1);
        LocalDate origEnd   = LocalDate.of(2026, 7, 3);
        TripPlan plan = makePlan(OWNER_ID, origStart, origEnd, 0L);

        TripDay day1 = makeDay(plan, 1);
        TripDay day2 = makeDay(plan, 2);
        TripDay day3 = makeDay(plan, 3);

        // day3에 더미 장소 추가
        Attraction attr = makeAttraction(100L, "C001", 12);
        com.trip.plan.entity.TripPlace place = com.trip.plan.entity.TripPlace.builder()
                .day(day3).attraction(attr).seq(1).build();
        day3.getPlaces().add(place);

        plan.getDays().addAll(java.util.List.of(day1, day2, day3));

        given(planRepository.findByIdWithDays(PLAN_ID)).willReturn(Optional.of(plan));

        // 2일로 축소
        PlanUpdateRequestDto req = new PlanUpdateRequestDto(
                0L, "줄인 여행", origStart, origStart.plusDays(1), null, null, null, null);

        // when / then
        assertThatThrownBy(() -> planService.update(OWNER_ID, PLAN_ID, req))
                .isInstanceOf(PlanHandler.class)
                .satisfies(ex -> assertThat(((PlanHandler) ex).getErrorCode())
                        .isEqualTo(ResponseCode.PLAN_PERIOD_CONFLICT));
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 버전 불일치 → 409
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: expectedVersion 불일치 → PLAN_VERSION_CONFLICT 409")
    void update_versionMismatch_throwsVersionConflict() {
        // given — plan version=2, client expects version=1
        TripPlan plan = makePlan(OWNER_ID,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 2L);
        plan.getDays().addAll(java.util.List.of(
                makeDay(plan, 1), makeDay(plan, 2), makeDay(plan, 3)));

        given(planRepository.findByIdWithDays(PLAN_ID)).willReturn(Optional.of(plan));

        PlanUpdateRequestDto req = new PlanUpdateRequestDto(
                1L,  // 불일치 expectedVersion
                "수정 여행",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 3),
                null, null, null, null);

        // when / then
        assertThatThrownBy(() -> planService.update(OWNER_ID, PLAN_ID, req))
                .isInstanceOf(PlanHandler.class)
                .satisfies(ex -> assertThat(((PlanHandler) ex).getErrorCode())
                        .isEqualTo(ResponseCode.PLAN_VERSION_CONFLICT));
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 중복 장소 추가 → 409
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addPlace: 같은 day에 이미 있는 attraction → DUPLICATE_PLACE 409")
    void addPlace_duplicateAttraction_throwsDuplicatePlace() {
        // given
        TripPlan plan = makePlan(OWNER_ID,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), 0L);
        TripDay day1 = makeDay(plan, 1);
        Attraction attr = makeAttraction(100L, "C001", 12);

        // 이미 같은 attraction이 day1에 존재
        com.trip.plan.entity.TripPlace existingPlace = com.trip.plan.entity.TripPlace.builder()
                .day(day1).attraction(attr).seq(1).build();
        day1.getPlaces().add(existingPlace);
        plan.getDays().add(day1);

        given(planRepository.findByIdWithDays(PLAN_ID)).willReturn(Optional.of(plan));
        given(attractionService.upsertSnapshot("C001", 12)).willReturn(attr);

        PlaceAddRequestDto req = new PlaceAddRequestDto("C001", 12, null, null);

        // when / then
        assertThatThrownBy(() -> planService.addPlace(OWNER_ID, PLAN_ID, 1, req))
                .isInstanceOf(PlanHandler.class)
                .satisfies(ex -> assertThat(((PlanHandler) ex).getErrorCode())
                        .isEqualTo(ResponseCode.DUPLICATE_PLACE));
    }

    // ─────────────────────────────────────────────────────────────
    // 6. 유효하지 않은 기간 → 400
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: start > end → INVALID_PLAN_PERIOD 400")
    void create_invalidPeriod_throwsBadRequest() {
        PlanCreateRequestDto req = new PlanCreateRequestDto(
                "잘못된 기간",
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 1),  // end < start
                null, null, null, null, null);

        assertThatThrownBy(() -> planService.create(OWNER_ID, req))
                .isInstanceOf(PlanHandler.class)
                .satisfies(ex -> assertThat(((PlanHandler) ex).getErrorCode())
                        .isEqualTo(ResponseCode.INVALID_PLAN_PERIOD));
    }
}
