package com.trip.recommend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trip.attraction.client.AttractionTourApiClient;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.service.AttractionService;
import com.trip.festival.repository.FestivalRepository;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.RecommendHandler;
import com.trip.plan.dto.PlanDetailResponseDto;
import com.trip.plan.entity.OriginType;
import com.trip.plan.service.PlanService;
import com.trip.recommend.dto.ItineraryDraft;
import com.trip.recommend.dto.ItineraryDraft.DayPlan;
import com.trip.recommend.dto.ItineraryDraft.PlaceRecommendation;
import com.trip.recommend.dto.RecommendRequestDto;
import com.trip.recommend.dto.RecommendationResponseDto;
import com.trip.recommend.entity.Recommendation;
import com.trip.recommend.entity.RecommendStatus;
import com.trip.recommend.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendServiceTest {

    @Mock private RecommendationRepository recommendationRepository;
    @Mock private AttractionTourApiClient  tourApiClient;
    @Mock private FestivalRepository       festivalRepository;
    @Mock private AttractionService        attractionService;
    @Mock private PlanService              planService;
    @Mock private StringRedisTemplate      stringRedisTemplate;
    @Mock private ChatClient               chatClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @InjectMocks
    private RecommendService recommendService;

    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec      callSpec;

    private static final Long   USER_ID = 1L;
    private static final Long   RECO_ID = 10L;
    private static final String AREA    = "6";  // 부산

    private RecommendRequestDto makeReq() {
        return new RecommendRequestDto(AREA,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                "COUPLE", 200000, List.of("자연"));
    }

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼: Recommendation 엔티티 생성 (reflection으로 id 주입)
    // ─────────────────────────────────────────────────────────────

    private Recommendation makeRec(Long id, Long userId, RecommendStatus status,
                                   String resultJson, Long savedPlanId) {
        Recommendation rec = Recommendation.builder()
                .userId(userId)
                .requestJson("{}")
                .requestHash("hash")
                .resultJson(resultJson)
                .model("gpt-4o-mini")
                .status(status)
                .savedPlanId(savedPlanId)
                .build();
        setField(rec, "id", id);
        setField(rec, "createdAt", LocalDateTime.now());
        return rec;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            // Walk the class hierarchy to find the field
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + name);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 테스트 1: 락 점유 중 409 RECO_IN_PROGRESS
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Redis 락 이미 점유 중이면 409 RECO_IN_PROGRESS")
    void process_lockAlreadyHeld_throws409() {
        RecommendRequestDto req = makeReq();

        // 캐시 미스
        given(recommendationRepository
                .findFirstByUserIdAndRequestHashAndStatusAndCreatedAtAfter(
                        anyLong(), anyString(), eq(RecommendStatus.SUCCESS), any()))
                .willReturn(Optional.empty());

        // 락 획득 실패 (NX → false)
        given(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .willReturn(false);

        assertThatThrownBy(() -> recommendService.process(USER_ID, req))
                .isInstanceOf(RecommendHandler.class)
                .extracting(e -> ((RecommendHandler) e).getErrorCode())
                .isEqualTo(ResponseCode.RECO_IN_PROGRESS);

        // LLM 호출 없어야 함
        verifyNoInteractions(chatClient);
    }

    // 12-인자 AttractionItem(테스트용) — contentId·type·title만 의미 있고 나머지는 null
    private static AttractionItem item(String id, String type, String title) {
        return new AttractionItem(id, type, title, "주소", AREA, null, null, null, null, null, null, null);
    }

    // ─────────────────────────────────────────────────────────────
    // 테스트 2: 검증 로직 — 후보 외 contentId 제거 후 PARTIAL
    //   (에이전트형 전환 후 후보 수집은 도구가 담당 → 결정론적 후처리(validate/computeStatus)를 직접 검증)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate: 후보 외 contentId는 제거되고, 일자당 2곳 미만이면 PARTIAL")
    void validate_unknownContentIdFiltered_partial() {
        Set<String> candidateIds = Set.of("111");
        List<AttractionItem> candidates = List.of(item("111", "12", "관광지A"));

        // 초안: "111"(유효) + "999"(후보 외). day2는 "999"뿐
        ItineraryDraft draft = new ItineraryDraft(
                List.of(
                        new DayPlan(1, List.of(
                                new PlaceRecommendation("111", "관광지A", "10:00", "좋은 곳"),
                                new PlaceRecommendation("999", "없는곳",  "11:00", "이유")
                        ), "day1"),
                        new DayPlan(2, List.of(
                                new PlaceRecommendation("999", "없는곳2", "09:00", "이유2")
                        ), "day2")
                ),
                "전체"
        );

        ItineraryDraft validated = recommendService.validate(draft, candidateIds, candidates, 2);

        // day1: "999" 제거 → "111"만 남음
        assertThat(validated.days().get(0).places())
                .extracting(PlaceRecommendation::contentId)
                .containsExactly("111");
        // 보충 후보가 없어 일자당 2곳을 못 채움 → PARTIAL
        assertThat(recommendService.computeStatus(validated, 2))
                .isEqualTo(RecommendStatus.PARTIAL);
    }

    // ─────────────────────────────────────────────────────────────
    // 테스트 3: 모든 장소가 후보 외 + 보충 후보도 없음 → 빈 결과 → FAILED
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate+computeStatus: 후보가 전혀 없으면 빈 결과 → FAILED")
    void validate_allUnknown_failed() {
        Set<String> candidateIds = Set.of();        // 유효 후보 없음
        List<AttractionItem> candidates = List.of(); // 보충할 후보도 없음

        ItineraryDraft draft = new ItineraryDraft(
                List.of(new DayPlan(1, List.of(
                        new PlaceRecommendation("999", "없는곳", null, "이유")
                ), "day1")),
                "전체"
        );

        ItineraryDraft validated = recommendService.validate(draft, candidateIds, candidates, 2);

        assertThat(recommendService.computeStatus(validated, 2))
                .isEqualTo(RecommendStatus.FAILED);
    }

    // ─────────────────────────────────────────────────────────────
    // 테스트 4: save-plan 멱등 — savedPlanId 존재 시 LLM·plan 생성 안 함
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("savedPlanId 이미 있으면 planService.getDetail만 호출하고 LLM·createFromDraft 호출 없음")
    void savePlan_alreadySaved_idempotent() {
        Recommendation rec = makeRec(RECO_ID, USER_ID, RecommendStatus.SUCCESS,
                "{}", 77L /* savedPlanId 이미 있음 */);

        given(recommendationRepository.findByIdForUpdate(RECO_ID)).willReturn(Optional.of(rec));

        PlanDetailResponseDto mockPlan = new PlanDetailResponseDto(
                77L, "기존 계획",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2),
                null, null, OriginType.AI, 0L,
                LocalDateTime.now(), LocalDateTime.now(), List.of()
        );
        given(planService.getDetail(USER_ID, 77L)).willReturn(mockPlan);

        PlanDetailResponseDto result = recommendService.savePlan(USER_ID, RECO_ID);

        assertThat(result.id()).isEqualTo(77L);

        // LLM 및 createFromDraft 호출 없어야 함
        verifyNoInteractions(chatClient);
        verify(planService, never()).createFromDraft(any(), any(), any());
        verify(planService, times(1)).getDetail(USER_ID, 77L);
    }
}
