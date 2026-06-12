package com.ssafy.ai.tool;

import com.ssafy.ai.service.WebSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class WebSearchTool {

    private final WebSearchService webSearchService;

    @Tool(description = """
            웹 검색을 수행하고 상위 결과를 요약해 반환한다.
            입력 예: "최신 AI 기술 트렌드", "Spring AI 예제"
            """)
    public String webSearch(String query) {
        return webSearchService.searchTop3(query);
    }
}
