package com.ssafy.ai.planner;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ssafy.ai.tool.WebSearchTool;
import com.ssafy.ai.tool.UserTools;
import com.ssafy.ai.tool.WeatherTool;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ToolExecutor {

    private final WeatherTool weatherTool;
    private final WebSearchTool webSearchTool;
    private final UserTools userTools;


    public Map<String, String> execute(Plan plan) {
        Map<String, String> results = new LinkedHashMap<>();

        for (int i = 0; i < plan.getSteps().size(); i++) {
            PlanStep step = plan.getSteps().get(i);

            String key = (i + 1) + ". " + step.getTool().name();
            String value = switch (step.getTool()) {
                case WEATHER -> {
                    String loc = (String) step.getArgs().getOrDefault("location", "서울");
                    yield weatherTool.getWeather(loc);
                }
                case WEB_SEARCH -> {
                    String q = (String) step.getArgs().getOrDefault("query", "");
                    yield webSearchTool.webSearch(q);
                }
                case USERS_COUNT -> {
                    String c = (String) step.getArgs().getOrDefault("criteriaText", "");
                    yield userTools.countUsersByText(c);
                }
                case USERS_STATS -> {
                    String c = (String) step.getArgs().getOrDefault("criteriaText", "");
                    String g = (String) step.getArgs().getOrDefault("groupBy", "region");
                    yield userTools.getUserGroupStatsByText(c, g);
                }
                case USERS_RECENT -> {
                    Integer limit = (Integer) step.getArgs().getOrDefault("limit", 10);
                    yield userTools.listRecentUsers(limit);
                }
                case USERS_SUMMARY -> {
                    String c = (String) step.getArgs().getOrDefault("criteriaText", "");
                    Integer limit = (Integer) step.getArgs().getOrDefault("limit", 10);
                    yield userTools.summarizeUsersByText(c, limit);
                }
            };

            results.put(key, value);
        }

        return results;
    }
}
