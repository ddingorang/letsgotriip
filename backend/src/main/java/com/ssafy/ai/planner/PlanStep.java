package com.ssafy.ai.planner;

import java.util.Map;

public class PlanStep {

    public enum ToolName {
        WEATHER,
        WEB_SEARCH,
        USERS_COUNT,
        USERS_SUMMARY,
        USERS_STATS,
        USERS_RECENT
    }

    private final ToolName tool;
    private final Map<String, Object> args;

    public PlanStep(ToolName tool, Map<String, Object> args) {
        this.tool = tool;
        this.args = args;
    }

    public ToolName getTool() {
        return tool;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "PlanStep{tool=" + tool + ", args=" + args + "}";
    }
}
