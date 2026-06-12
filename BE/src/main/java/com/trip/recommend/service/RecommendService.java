package com.trip.recommend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.attraction.client.AttractionTourApiClient;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.service.AttractionService;
import com.trip.festival.entity.Festival;
import com.trip.festival.repository.FestivalRepository;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.RecommendHandler;
import com.trip.plan.dto.PlanDetailResponseDto;
import com.trip.plan.entity.CompanionsType;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 추천 도메인 서비스.
 *
 * 파이프라인:
 * 1. 동일 hash SUCCESS 캐시 확인 (5분)
 * 2. Redis 락 획득 (lock:reco:{userId}, NX EX 60)
 * 3. 후보 수집 (TourAPI + festivals)
 * 4. ChatClient + BeanOutputConverter로 초안 생성
 * 5. 검증 (§5 순서: 후보 외 제거 → dayNo 범위 → 중복 → 보충 → PARTIAL/FAILED)
 * 6. 결과 저장 (성공/실패 모두)
 *
 * LLM read timeout: spring.ai.openai의 기본 설정(30s)으로 충분 — GMS 프록시 기준.
 * 별도 RestClient 빌더 오버라이드 미사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendService {

    private static final String LOCK_PREFIX      = "lock:reco:";
    // LLM 최대 30s + TourAPI 호출 + DB 커밋 여유를 감안해 60s → 120s로 상향
    private static final long   LOCK_TTL_SECONDS = 120L;
    private static final int    MAX_PAGE_SIZE     = 50;
    private static final String MODEL_NAME        = "gpt-4o-mini";

    // Lua compare-and-delete: 값이 일치할 때만 삭제 (원자적 해제)
    private static final String LUA_COMPARE_AND_DELETE =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final RecommendationRepository recommendationRepository;
    private final AttractionTourApiClient  tourApiClient;
    private final FestivalRepository       festivalRepository;
    private final AttractionService        attractionService;
    private final PlanService              planService;
    private final StringRedisTemplate      stringRedisTemplate;
    private final ChatClient               chatClient;
    private final ObjectMapper             objectMapper;

    // ─────────────────────────────────────────────────────────────
    // 추천 생성
    // ─────────────────────────────────────────────────────────────

    // NOT_SUPPORTED: 클래스 레벨 @Transactional(readOnly=true)를 무력화해 process를 트랜잭션 밖에서
    // 실행한다. 이렇게 해야 (1) 외부 호출(TourAPI/LLM) 동안 DB 커넥션을 점유하지 않고,
    // (2) 성공/실패 Recommendation 저장이 repository.save 자체 쓰기 트랜잭션으로 커밋되며
    //     (readOnly 커넥션 INSERT 오류 방지), (3) 실패 기록이 예외 재던지기로 롤백되지 않는다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RecommendationResponseDto process(Long userId, RecommendRequestDto req) {
        // 기간 유효성 검사
        if (!req.isPeriodValid()) {
            throw new RecommendHandler(ResponseCode._BAD_REQUEST);
        }

        // 1. 동일 hash SUCCESS 캐시 (5분)
        String requestJson = serializeRequest(req);
        String requestHash = sha256(requestJson);

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        Optional<Recommendation> cached = recommendationRepository
                .findFirstByUserIdAndRequestHashAndStatusAndCreatedAtAfter(
                        userId, requestHash, RecommendStatus.SUCCESS, fiveMinutesAgo);
        if (cached.isPresent()) {
            log.info("추천 캐시 히트 — userId={}, hash={}", userId, requestHash);
            return RecommendationResponseDto.from(cached.get(), objectMapper);
        }

        // 2. Redis 락 획득
        String lockKey   = LOCK_PREFIX + userId;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new RecommendHandler(ResponseCode.RECO_IN_PROGRESS);
        }

        long startMs = System.currentTimeMillis();
        Recommendation saved = null;
        try {
            // 3. 후보 수집
            List<AttractionItem> candidates = collectCandidates(req.areaCode());

            // attraction 후보(관광지/문화시설/음식점)가 전혀 없으면 LLM 호출 없이 실패
            boolean hasAttractionCandidates = candidates.stream()
                    .anyMatch(i -> i.contentId() != null && !"15".equals(i.contentTypeId()));
            if (!hasAttractionCandidates) {
                throw new RecommendHandler(ResponseCode.AI_GENERATION_FAILED);
            }

            Set<String> candidateIds = candidates.stream()
                    .map(AttractionItem::contentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 4. LLM 호출
            ItineraryDraft draft = callLlm(req, candidates);

            // 5. 검증
            ItineraryDraft validated = validate(draft, candidateIds, candidates, req.totalDays());

            // 최종 상태 판별
            RecommendStatus status = computeStatus(validated, req.totalDays());

            if (status == RecommendStatus.FAILED) {
                int latency = (int) (System.currentTimeMillis() - startMs);
                saved = recommendationRepository.save(Recommendation.builder()
                        .userId(userId)
                        .requestJson(requestJson)
                        .requestHash(requestHash)
                        .resultJson(null)
                        .model(MODEL_NAME)
                        .status(RecommendStatus.FAILED)
                        .errorCode(ResponseCode.RECO_EMPTY_RESULT.getCode())
                        .errorMessage(ResponseCode.RECO_EMPTY_RESULT.getMessage())
                        .latencyMs(latency)
                        .build());
                throw new RecommendHandler(ResponseCode.RECO_EMPTY_RESULT);
            }

            String resultJson = serializeDraft(validated);
            int latency = (int) (System.currentTimeMillis() - startMs);
            saved = recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .requestJson(requestJson)
                    .requestHash(requestHash)
                    .resultJson(resultJson)
                    .model(MODEL_NAME)
                    .status(status)
                    .latencyMs(latency)
                    .build());

            return RecommendationResponseDto.from(saved, objectMapper);

        } catch (RecommendHandler e) {
            // FAILED 저장은 이미 위에서 처리됨, RECO_IN_PROGRESS는 락 미획득이므로 저장 불필요
            throw e;
        } catch (Exception e) {
            log.error("AI 추천 생성 실패 — userId={}, error={}", userId, e.getMessage(), e);
            int latency = (int) (System.currentTimeMillis() - startMs);
            recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .requestJson(requestJson)
                    .requestHash(requestHash)
                    .resultJson(null)
                    .model(MODEL_NAME)
                    .status(RecommendStatus.FAILED)
                    .errorCode(ResponseCode.AI_GENERATION_FAILED.getCode())
                    .errorMessage(e.getMessage())
                    .latencyMs(latency)
                    .build());
            throw new RecommendHandler(ResponseCode.AI_GENERATION_FAILED);
        } finally {
            // Lua compare-and-delete — 내 값일 때만 삭제
            releaseLock(lockKey, lockValue);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 이력 조회
    // ─────────────────────────────────────────────────────────────

    public Page<RecommendationResponseDto> getHistory(Long userId, int page, int size) {
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize);
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(r -> RecommendationResponseDto.from(r, objectMapper));
    }

    public RecommendationResponseDto getOne(Long userId, Long id) {
        Recommendation rec = findAndVerify(userId, id);
        return RecommendationResponseDto.from(rec, objectMapper);
    }

    // ─────────────────────────────────────────────────────────────
    // save-plan (멱등)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PlanDetailResponseDto savePlan(Long userId, Long recommendationId) {
        // 비관적 잠금으로 Recommendation 조회: 동시 savePlan 요청 시 savedPlanId 재확인 보장
        Recommendation rec = recommendationRepository.findByIdForUpdate(recommendationId)
                .orElseThrow(() -> new RecommendHandler(ResponseCode.RECO_NOT_FOUND));
        if (!rec.getUserId().equals(userId)) {
            throw new RecommendHandler(ResponseCode.RECO_FORBIDDEN);
        }

        // 멱등: 락 안에서 savedPlanId 재확인 — 동시 요청이 plan을 중복 생성하지 않도록
        if (rec.getSavedPlanId() != null) {
            return planService.getDetail(userId, rec.getSavedPlanId());
        }

        if (rec.getResultJson() == null) {
            throw new RecommendHandler(ResponseCode.RECO_EMPTY_RESULT);
        }

        ItineraryDraft draft;
        try {
            draft = objectMapper.readValue(rec.getResultJson(), ItineraryDraft.class);
        } catch (Exception e) {
            throw new RecommendHandler(ResponseCode.AI_GENERATION_FAILED);
        }

        // 요청 정보 역직렬화
        RecommendRequestDto req;
        try {
            req = objectMapper.readValue(rec.getRequestJson(), RecommendRequestDto.class);
        } catch (Exception e) {
            throw new RecommendHandler(ResponseCode.AI_GENERATION_FAILED);
        }

        PlanDetailResponseDto planDto = planService.createFromDraft(userId, draft, req);
        rec.markSavedPlan(planDto.id());
        // 동일 트랜잭션에서 recommendation 업데이트 반영됨 (dirty-check)

        return planDto;
    }

    // ─────────────────────────────────────────────────────────────
    // 내부: 후보 수집
    // ─────────────────────────────────────────────────────────────

    private List<AttractionItem> collectCandidates(String areaCode) {
        List<AttractionItem> result = new ArrayList<>();

        // contentTypeId 12(관광지), 14(문화시설), 39(음식점) × 각 15개
        for (String typeId : List.of("12", "14", "39")) {
            try {
                List<AttractionItem> items = tourApiClient.fetchAreaBased(areaCode, null, typeId, 1, 15);
                result.addAll(items);
            } catch (Exception e) {
                log.warn("TourAPI fetchAreaBased 실패 — areaCode={}, typeId={}, error={}", areaCode, typeId, e.getMessage());
            }
        }

        // 축제: areaCode + 오늘 기준 진행 중 or 예정 (최대 5개)
        try {
            List<Festival> festivals = festivalRepository.findByAreaCode(areaCode);
            LocalDate today = LocalDate.now();
            festivals.stream()
                    .filter(f -> f.getEndDate() != null && !f.getEndDate().isBefore(today))
                    .limit(5)
                    .forEach(f -> result.add(festivalToAttractionItem(f)));
        } catch (Exception e) {
            log.warn("Festival 조회 실패 — areaCode={}, error={}", areaCode, e.getMessage());
        }

        return result;
    }

    private AttractionItem festivalToAttractionItem(Festival f) {
        // Festival을 AttractionItem record 형태로 변환 (프롬프트 카탈로그용)
        return new AttractionItem(
                f.getContentId(),
                "15",           // contentTypeId: 축제
                f.getTitle(),
                f.getAddress(),
                f.getAreaCode(),
                f.getSigunguCode(),
                f.getLatitude()  != null ? String.valueOf(f.getLatitude())  : null,
                f.getLongitude() != null ? String.valueOf(f.getLongitude()) : null,
                f.getImageUrl(),
                f.getTel(),
                null            // overview
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 내부: LLM 호출
    // ─────────────────────────────────────────────────────────────

    private ItineraryDraft callLlm(RecommendRequestDto req, List<AttractionItem> candidates) {
        BeanOutputConverter<ItineraryDraft> converter = new BeanOutputConverter<>(ItineraryDraft.class);

        String catalog = candidates.stream()
                .filter(i -> i.contentId() != null && i.title() != null)
                .map(i -> String.format("%s|%s|%s|%s",
                        i.contentId(),
                        nvl(i.contentTypeId()),
                        i.title(),
                        nvl(i.addr1())))
                .collect(Collectors.joining("\n"));

        String userPrompt = buildPrompt(req, catalog, converter.getFormat());

        String raw = chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        return converter.convert(raw);
    }

    private String buildPrompt(RecommendRequestDto req, String catalog, String format) {
        return String.format("""
                당신은 여행 일정 전문가입니다. 아래 후보 장소 목록에서만 선택하여 %d일 여행 일정을 작성해주세요.

                [여행 조건]
                - 지역코드: %s
                - 기간: %s ~ %s (%d일)
                - 동행: %s
                - 예산: %s
                - 테마: %s

                [후보 장소 목록] (contentId|contentTypeId|title|addr)
                %s

                [제약 조건]
                - 반드시 위 후보 목록의 contentId만 사용할 것
                - 일자별 3~5곳 포함
                - visitTime은 반드시 "HH:mm" 형식으로 작성 (불확실하면 null)
                - dayNo는 1부터 %d까지

                [출력 형식]
                %s
                """,
                req.totalDays(),
                req.areaCode(),
                req.startDate(), req.endDate(), req.totalDays(),
                nvl(req.companions()),
                req.budget() != null ? req.budget() + "원" : "미지정",
                req.themes() != null ? String.join(", ", req.themes()) : "미지정",
                catalog,
                req.totalDays(),
                format
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 내부: 검증 (§5 순서)
    // ─────────────────────────────────────────────────────────────

    private ItineraryDraft validate(ItineraryDraft draft, Set<String> candidateIds,
                                    List<AttractionItem> allCandidates, int totalDays) {
        if (draft == null || draft.days() == null) {
            return new ItineraryDraft(List.of(), "");
        }

        // 1단계: 후보 외 contentId 제거 + dayNo 범위 밖 제거
        List<DayPlan> validDays = draft.days().stream()
                .filter(d -> d.dayNo() >= 1 && d.dayNo() <= totalDays)
                .map(d -> {
                    List<PlaceRecommendation> places = d.places() == null ? List.of() : d.places().stream()
                            .filter(p -> p.contentId() != null && candidateIds.contains(p.contentId()))
                            .collect(Collectors.toList());
                    return new DayPlan(d.dayNo(), places, d.summary());
                })
                .collect(Collectors.toList());

        // 2단계: 중복 contentId 1회만 (전체 일정 기준)
        Set<String> seen = new HashSet<>();
        validDays = validDays.stream()
                .map(d -> {
                    List<PlaceRecommendation> deduped = d.places().stream()
                            .filter(p -> seen.add(p.contentId()))
                            .collect(Collectors.toList());
                    return new DayPlan(d.dayNo(), deduped, d.summary());
                })
                .collect(Collectors.toList());

        // 3단계: 일자당 2곳 미만 → 후보 풀에서 같은 type 장소로 1회 보충
        List<DayPlan> supplemented = new ArrayList<>();
        for (DayPlan day : validDays) {
            if (day.places().size() < 2) {
                List<PlaceRecommendation> places = new ArrayList<>(day.places());
                // 현재 일자 type 분포로 보충 후보 탐색
                List<AttractionItem> supplements = allCandidates.stream()
                        .filter(c -> c.contentId() != null && !seen.contains(c.contentId()))
                        .limit(2 - places.size())
                        .collect(Collectors.toList());
                for (AttractionItem s : supplements) {
                    seen.add(s.contentId());
                    places.add(new PlaceRecommendation(
                            s.contentId(),
                            s.title(),
                            null,
                            "보충 장소"
                    ));
                }
                supplemented.add(new DayPlan(day.dayNo(), places, day.summary()));
            } else {
                supplemented.add(day);
            }
        }

        return new ItineraryDraft(supplemented, draft.totalSummary());
    }

    private RecommendStatus computeStatus(ItineraryDraft validated, int totalDays) {
        if (validated.days() == null || validated.days().isEmpty()) {
            return RecommendStatus.FAILED;
        }
        // 전체가 비어 있으면 FAILED
        boolean allEmpty = validated.days().stream().allMatch(d -> d.places() == null || d.places().isEmpty());
        if (allEmpty) return RecommendStatus.FAILED;

        // 일자 수 부족 또는 일자당 2곳 미만 → PARTIAL
        if (validated.days().size() < totalDays) return RecommendStatus.PARTIAL;
        boolean anyShort = validated.days().stream().anyMatch(d -> d.places().size() < 2);
        return anyShort ? RecommendStatus.PARTIAL : RecommendStatus.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────
    // 내부: 공통 헬퍼
    // ─────────────────────────────────────────────────────────────

    private Recommendation findAndVerify(Long userId, Long id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new RecommendHandler(ResponseCode.RECO_NOT_FOUND));
        if (!rec.getUserId().equals(userId)) {
            throw new RecommendHandler(ResponseCode.RECO_FORBIDDEN);
        }
        return rec;
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_COMPARE_AND_DELETE, Long.class);
            stringRedisTemplate.execute(script, List.of(lockKey), lockValue);
        } catch (Exception e) {
            log.warn("Redis 락 해제 실패 — key={}, error={}", lockKey, e.getMessage());
        }
    }

    private String serializeRequest(RecommendRequestDto req) {
        // 정규화된 JSON (TreeMap으로 키 정렬)
        try {
            TreeMap<String, Object> map = new TreeMap<>();
            map.put("areaCode",   req.areaCode());
            map.put("startDate",  req.startDate() != null ? req.startDate().toString() : null);
            map.put("endDate",    req.endDate()   != null ? req.endDate().toString()   : null);
            map.put("companions", req.companions());
            map.put("budget",     req.budget());
            map.put("themes",     req.themes() != null ? new TreeSet<>(req.themes()) : null);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("요청 직렬화 실패", e);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String serializeDraft(ItineraryDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (Exception e) {
            throw new IllegalStateException("결과 직렬화 실패", e);
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
