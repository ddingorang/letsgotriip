package com.ssafy.ai.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class WebSearchService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${api.tavily.key}")
    private String apiKey;

    public String searchTop3(String query) {
        try {
            Map<String, Object> body = Map.of(
                "api_key", apiKey,
                "query", query,
                "max_results", 3
            );

            String response = restClient.post()
                .uri("https://api.tavily.com/search")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                return "검색 결과가 없습니다.";
            }

            StringBuilder sb = new StringBuilder("[검색 결과]\n");
            int idx = 1;
            for (JsonNode item : results) {
                if (idx > 3) break;
                sb.append(idx++).append(". ")
                    .append(item.path("title").asText()).append("\n")
                    .append("   ").append(item.path("content").asText("")).append("\n");
            }
            return sb.toString().trim();

        } catch (Exception e) {
            log.error("Tavily 검색 실패: {}", e.getMessage());
            return "검색 실패: " + e.getMessage();
        }
    }
}
