package com.trip.preprocessing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * STT/카카오톡 분석 rawText에서 여행 취향 테마 키를 추출한다.
 *
 * <p>추천 도메인(RecommendService.THEME_LABELS)이 사용하는 6개 테마 키
 * (sea/mountain/food/history/activity/shopping) 중 rawText에 드러난 성향만 골라낸다.
 * LLM 호출은 워치독 스레드 + {@link #CALL_TIMEOUT_SECONDS} 상한으로 감싸 무한 대기를 막고,
 * 실패·타임아웃·빈 결과는 모두 빈 리스트로 graceful 처리한다(예외 전파 없음).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelPreferenceExtractor {

    /** 추천 도메인 THEME_LABELS와 동일한 허용 테마 키 집합(BE 단일 소스). */
    private static final Set<String> ALLOWED_THEME_KEYS =
            Set.of("sea", "mountain", "food", "history", "activity", "shopping");

    /** rawText가 너무 길면 프롬프트 비용/지연을 줄이기 위해 앞부분만 사용. */
    private static final int MAX_TEXT_LENGTH = 4000;

    /** LLM 호출 전체 상한(초). 무응답 시 업로드 흐름을 막지 않도록 짧게 둔다. */
    private static final long CALL_TIMEOUT_SECONDS = 20;

    private static final String PROMPT_TEMPLATE = """
            아래는 여행 관련 대화/통화 전사 텍스트야. 이 사람이 드러낸 여행 취향을 아래 6개 테마 키 중에서만 골라줘.
            - sea: 바다·해변
            - mountain: 산·자연
            - food: 맛집·미식
            - history: 역사·문화 유적
            - activity: 액티비티·체험
            - shopping: 쇼핑

            규칙:
            - 텍스트에서 분명히 드러난 취향만 선택할 것(추측 금지).
            - 해당하는 키를 콤마로만 구분해서 출력(예: food,sea).
            - 위 6개 키 외의 단어, 설명, 문장은 절대 출력하지 말 것.
            - 해당하는 취향이 없으면 빈 줄(아무것도 출력하지 않음)로 응답할 것.

            [텍스트]
            %s
            """;

    private final ChatClient chatClient;

    /** LLM 호출 워치독용 데몬 워커. */
    private final ExecutorService callExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "preference-extractor-llm");
        t.setDaemon(true);
        return t;
    });

    /**
     * rawText에서 허용된 테마 키 목록을 추출한다. 실패·타임아웃·빈 결과는 빈 리스트로 반환한다.
     *
     * @param rawText 마스킹된 분석 텍스트
     * @return 추출된 테마 키(중복 제거, 입력 순서 보존). 추출 불가 시 빈 리스트.
     */
    public List<String> extractThemeKeys(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        String text = rawText.length() > MAX_TEXT_LENGTH
                ? rawText.substring(0, MAX_TEXT_LENGTH)
                : rawText;
        String prompt = String.format(PROMPT_TEMPLATE, text);

        String raw;
        Future<String> future = callExecutor.submit(() ->
                chatClient.prompt().user(prompt).call().content());
        try {
            raw = future.get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("취향 추출 LLM 호출 타임아웃 — {}s 초과", CALL_TIMEOUT_SECONDS);
            return List.of();
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            log.warn("취향 추출 LLM 호출 실패 — error={}", e.getMessage());
            return List.of();
        }

        return parseThemeKeys(raw);
    }

    /** LLM 응답을 콤마 기준으로 파싱하고 허용된 키만 중복 없이 추린다. */
    private List<String> parseThemeKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String token : Arrays.asList(raw.toLowerCase().split("[^a-z]+"))) {
            String key = token.trim();
            if (ALLOWED_THEME_KEYS.contains(key)) {
                result.add(key);
            }
        }
        return new ArrayList<>(result);
    }
}
