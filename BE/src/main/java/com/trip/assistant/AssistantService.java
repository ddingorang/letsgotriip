package com.trip.assistant;

import com.trip.attraction.dto.AttractionSearchRequestDto;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.service.AttractionService;
import com.trip.plan.dto.PlanDetailResponseDto;
import com.trip.recommend.dto.RecommendRequestDto;
import com.trip.recommend.dto.RecommendationResponseDto;
import com.trip.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Triip 대화형 여행 어시스턴트.
 *
 * <p>RAG(QuestionAnswerAdvisor) + 대화기억(MessageChatMemoryAdvisor) + 함수호출(@Tool)을 결합한다.
 * <ul>
 *   <li>RAG: 사용자의 문서/여행기록 청크를 userId 필터로 검색해 컨텍스트에 주입</li>
 *   <li>기억: conversationId별 최근 메시지 윈도우 유지</li>
 *   <li>도구: 관광지 검색 / AI 여행계획 생성·저장</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private static final String SYSTEM_PROMPT = """
            너는 Triip 여행 어시스턴트야.
            사용자의 문서와 여행기록을 참고해 항상 한국어로 친절하고 간결하게 답해.
            필요하면 제공된 도구를 사용해 관광지를 검색하거나 여행 일정을 제안/생성해.
            여행계획 생성 도구는 사용자가 명확히 일정 생성을 원할 때만 사용하고,
            지역코드(areaCode)·기간(startDate~endDate)이 분명할 때만 호출해.
            확실하지 않은 정보는 지어내지 말고 사용자에게 되물어봐.
            """;

    private static final int TOP_K = 4;

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    // 도구 백엔드 — 읽기 전용 의존(해당 서비스 파일은 수정하지 않음)
    private final AttractionService attractionService;
    private final RecommendService recommendService;

    /**
     * 사용자 메시지를 처리하고 어시스턴트 응답 텍스트를 반환한다.
     *
     * @param userId         인증 사용자 — RAG 검색 격리 및 도구 컨텍스트
     * @param conversationId 대화 식별자(기억 윈도우 키)
     * @param message        사용자 입력
     */
    public String chat(Long userId, String conversationId, String message) {
        // userId로 격리된 RAG 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .filterExpression("userId == '" + userId + "'")
                .topK(TOP_K)
                .build();

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build();

        return chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(qaAdvisor, memoryAdvisor)
                .tools(new AssistantTools(userId))
                .call()
                .content();
    }

    /**
     * LLM 함수호출용 도구 묶음. 요청별 userId를 캡처해 도구가 사용자 컨텍스트를 안전하게 사용하도록 한다
     * (userId를 LLM이 채우는 파라미터로 노출하지 않는다).
     */
    public class AssistantTools {

        private final Long userId;

        AssistantTools(Long userId) {
            this.userId = userId;
        }

        @Tool(description = "키워드 또는 지역코드로 한국 관광지/맛집/문화시설을 검색한다. "
                + "결과는 장소명과 주소 목록이다.")
        public String searchAttractions(
                @ToolParam(required = false, description = "검색 키워드(예: '경복궁', '해운대'). 없으면 지역 기반 검색")
                String keyword,
                @ToolParam(required = false, description = "지역코드(예: 서울 1, 부산 6). 키워드가 없을 때 사용")
                String areaCode,
                @ToolParam(required = false, description = "콘텐츠 유형 코드(12 관광지, 14 문화시설, 39 음식점)")
                String contentTypeId) {
            try {
                AttractionSearchRequestDto req = new AttractionSearchRequestDto(
                        areaCode, null, contentTypeId, keyword, 1, 10, null, null, null);
                List<AttractionItem> items = attractionService.search(req);
                if (items.isEmpty()) {
                    return "검색 결과가 없습니다.";
                }
                return items.stream()
                        .limit(10)
                        .map(i -> "- " + nvl(i.title())
                                + (i.addr1() != null ? " (" + i.addr1() + ")" : "")
                                + " [contentId=" + nvl(i.contentId()) + "]")
                        .collect(Collectors.joining("\n"));
            } catch (Exception e) {
                log.warn("도구 searchAttractions 실패 — error={}", e.getMessage());
                return "관광지 검색 중 오류가 발생했습니다.";
            }
        }

        @Tool(description = "지역코드와 여행 기간을 받아 AI 여행 일정을 생성하고 저장한다. "
                + "저장된 일정 ID와 요약을 돌려준다. 기간은 최대 7일이다.")
        public String createTravelPlan(
                @ToolParam(description = "지역코드(예: 서울 1, 부산 6)")
                String areaCode,
                @ToolParam(description = "여행 시작일 (YYYY-MM-DD)")
                String startDate,
                @ToolParam(description = "여행 종료일 (YYYY-MM-DD)")
                String endDate,
                @ToolParam(required = false, description = "동행 유형(SOLO, COUPLE, FAMILY, FRIENDS)")
                String companions,
                @ToolParam(required = false, description = "총 예산(원)")
                Integer budget,
                @ToolParam(required = false, description = "선호 테마 콤마 구분(sea, mountain, food, history, activity, shopping)")
                String themes) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                List<String> themeList = (themes == null || themes.isBlank())
                        ? List.of()
                        : List.of(themes.split("\\s*,\\s*"));

                RecommendRequestDto req = new RecommendRequestDto(
                        areaCode, start, end, companions, budget, themeList);

                RecommendationResponseDto reco = recommendService.process(userId, req);
                PlanDetailResponseDto plan = recommendService.savePlan(userId, reco.id());

                return "여행 일정을 생성했어요. planId=" + plan.id()
                        + ", 제목='" + plan.title() + "'. 마이페이지에서 확인하고 수정할 수 있어요.";
            } catch (Exception e) {
                log.warn("도구 createTravelPlan 실패 — error={}", e.getMessage());
                return "일정 생성에 실패했습니다. 지역코드와 기간(최대 7일)을 확인해 주세요.";
            }
        }

        private String nvl(String s) {
            return s == null ? "" : s;
        }
    }
}
