package com.ssafy.ai.planner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AiPlanner {

    private final ChatClient chatClient;
    private final AiPlannerAssistant assistant;

    private static final String SYSTEM_PROMPT = """
            너는 tool planner다.
            - 오직 JSON만 출력한다. 설명 문장은 금지한다.
            - 출력 형식은 반드시 {"steps": [...] } 구조를 지켜야 한다. (매우 중요)
            - 도구가 하나여도 반드시 steps 리스트 안에 넣는다.
            - 사용자 요청을 읽고 필요한 step만 선택한다.
            - tool은 허용 목록에서만 고른다.

            [출력 예시]
            {"steps": [{"tool": "WEATHER", "args": {"location": "서울"}}]}

            [허용 tool 목록]

            WEATHER(args: location)
            WEB_SEARCH(args: query)
            USERS_COUNT(args: criteriaText)
            USERS_STATS(args: criteriaText, groupBy)
            USERS_RECENT(args: limit)
            USERS_SUMMARY(args: criteriaText, limit)

            [규칙]
            - 날씨 관련 의도면 WEATHER 우선.
            - 최신/근거/뉴스/검색 관련 의도면 WEB_SEARCH 포함.
            - 회원/유저/통계/요약 관련 의도면 USERS_* tool 포함.
            - 불필요한 tool 호출 금지.
            - args는 최소 필수 값만 넣는다.
            """;

    public Plan makePlan(String userMessage) {
        log.info(">>>> [AI Planning Start] message: {}", userMessage);

        // 1. LLM에게 실행 계획 요청 및 객체로 직접 변환 (Structured Output)
        AiPlannerAssistant.AiPlanDraft draft = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .entity(AiPlannerAssistant.AiPlanDraft.class);

        log.info("<<<< [AI Planning Draft Received]: {}", draft);

        // 2. 검증 및 내부 Plan 객체로 변환
        AiPlannerAssistant.ValidationResult result = assistant.validateAndConvert(draft);

        if (!result.valid()) {
            log.warn("!!!! [AI Planning Failed] Errors: {}", result.errors());
            // 최소한의 기본 계획으로 fallback (검색 시도)
            return new Plan();
        }

        log.info("<<<< [AI Planning Success] Steps count: {}", result.plan().getSteps().size());
        return result.plan();
    }
}
