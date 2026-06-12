package com.ssafy.ai.controller;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ai.tool.WebSearchTool;
import com.ssafy.ai.tool.UserTools;
import com.ssafy.ai.tool.WeatherTool;
import com.ssafy.ai.planner.AiPlanner;
import com.ssafy.ai.planner.Plan;
import com.ssafy.ai.planner.ToolExecutor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final String SYS_RULES = """
            너는 Tool(도구 호출) 기반 에이전트다.
            - 추측 금지: 외부 정보(날씨/웹검색)나 DB 정보(회원)는 반드시 Tool로 조회한 뒤 답하라.
            - 한 요청에서 Tool을 여러 번 연쇄 호출할 수 있다. (multi-tool)
            - 같은 Tool을 입력만 바꿔서 여러 번 재시도하는 행동은 금지한다. (최대 1회 호출 원칙)
              단, 사용자가 명확히 여러 지역/여러 검색어를 요청한 경우는 예외.

            [사용 가능한 Tool]
            (회원 DB)
            - countUsersByText(criteriaText)
            - getUserGroupStatsByText(criteriaText, groupBy)
            - summarizeUsersByText(criteriaText, limit)
            - listRecentUsers(limit)

            (날씨)
            - getWeather(city)

            (웹 검색)
            - webSearch(query)

            [Tool 선택 규칙]
            1) 사용자의 질문에 "날씨/기온/비/눈/옷차림/우산/외출" 등이 포함되면 getWeather(city)를 호출한다.
               - city는 질문에서 나온 도시/지역명을 그대로 사용한다. (예: "서울", "부산")
            2) 사용자의 질문에 "검색/찾아줘/최신/뉴스/트렌드/근거" 등이 포함되면 webSearch(query)를 호출한다.
               - query는 사용자의 문장을 핵심 검색어로 압축해서 넣는다.
            3) 사용자의 질문에 "회원/유저/우리 사람들/직군/지역/몇 명/요약/분포/통계" 등이 포함되면 회원 Tool을 호출한다.
            4) 하나의 요청에 위 조건이 여러 개 포함되면 필요한 Tool을 모두 호출하고, 결과를 종합해 답하라.

            [응답 형식]
            - 사용자가 복합 질문을 하면 아래 섹션으로 나눠서 답한다.
            (1) 조회 결과 (Tool 기반 사실)
            (2) 최종 메시지 (사실 기반의 짧은 판단)

            [예시]
            - "서울 날씨 알려주고, 최신 AI 트렌드 검색해서 개발 직군에게 공유할 포인트 3개 정리"
              => getWeather("서울") + webSearch("최신 AI 트렌드 2025") + (결과 정리)
            """;

    private final ChatClient chatClient;
    private final UserTools userTools;
    private final WeatherTool weatherTool;
    private final WebSearchTool webSearchTool;
    //////////////////////////////////////////////////////////////
    private final AiPlanner aiPlanner;
    private final ToolExecutor toolExecutor;

    //////////////////////////////////////////////////////////////
    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest req) {
        return chatClient.prompt()
                .system(SYS_RULES)
                .user(req.getMessage())
                .tools(userTools, weatherTool, webSearchTool)
                .call()
                .content();
    }

    @PostMapping("/smart-plan-chat")
    public String smartPlanChat(@RequestBody ChatRequest req) {
        String userMsg = req.getMessage();

        // 1) AI 기반 계획 생성 (Smart!)
        Plan plan = aiPlanner.makePlan(userMsg);

        // 2) 계획대로 tool 실행 (서버가 실행)
        Map<String, String> toolResults = toolExecutor.execute(plan);

        // 3) LLM은 결과 정리만 수행
        String system = """
                너는 정리/작성 전용 어시스턴트다.
                - 아래 [TOOL_RESULTS]는 서버가 실행한 결과다. 사실로 취급한다.
                - 사용자의 요청에 맞게 (1) 조회 결과 (2) 최종 메시지/추천 형태로 정리하라.
                - 새로운 사실을 만들어내지 마라. 부족하면 "추가 정보가 필요"라고 말하라.
                """;

        String prompt = """
                [USER_REQUEST]
                %s

                [TOOL_RESULTS]
                %s
                """.formatted(userMsg, pretty(toolResults));

        return chatClient.prompt()
                .system(system)
                .user(prompt)
                .call()
                .content();
    }

    private String pretty(Map<String, String> toolResults) {

        StringBuilder sb = new StringBuilder();
        for (var e : toolResults.entrySet()) {
            sb.append("[").append(e.getKey()).append("]\n")
                    .append(e.getValue()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
