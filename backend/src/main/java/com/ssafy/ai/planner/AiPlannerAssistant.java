package com.ssafy.ai.planner;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI 플래너의 보조 역할을 수행 (검증 및 변환)
 */
@Component
public class AiPlannerAssistant {

    public AiPlannerAssistant() {
    }

    // ==========================================
    // 1. AiPlanDraft 검증
    // ==========================================
    public ValidationResult validateAndConvert(AiPlanDraft draft) {
        List<String> errors = new ArrayList<>();
        Plan converted = new Plan();

        if (draft == null || draft.getSteps() == null || draft.getSteps().isEmpty()) {
            errors.add("steps must not be empty");
            return new ValidationResult(false, errors, converted);
        }

        for (int i = 0; i < draft.getSteps().size(); i++) {
            AiPlanDraft.Step step = draft.getSteps().get(i);
            int idx = i + 1;

            if (step == null) {
                errors.add("step[" + idx + "] is null");
                continue;
            }

            String toolRaw = (step.getTool() == null) ? "" : step.getTool().trim();
            PlanStep.ToolName tool = parseTool(toolRaw);
            if (tool == null) {
                errors.add("step[" + idx + "] invalid tool: " + toolRaw);
                continue;
            }

            Map<String, Object> args = (step.getArgs() == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(step.getArgs());
            List<String> argErrors = validateArgs(tool, args, idx);
            if (!argErrors.isEmpty()) {
                errors.addAll(argErrors);
                continue;
            }

            converted.add(new PlanStep(tool, args));
        }

        boolean ok = errors.isEmpty() && !converted.getSteps().isEmpty();
        return new ValidationResult(ok, errors, converted);
    }

    private PlanStep.ToolName parseTool(String toolRaw) {
        try {
            return PlanStep.ToolName.valueOf(toolRaw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> validateArgs(PlanStep.ToolName tool, Map<String, Object> args, int index) {
        List<String> errors = new ArrayList<>();
        switch (tool) {
        case WEATHER -> {
            if (asString(args.get("location")).isBlank())
                errors.add("step[" + index + "] WEATHER requires location");
        }
        case WEB_SEARCH -> {
            if (asString(args.get("query")).isBlank())
                errors.add("step[" + index + "] WEB_SEARCH requires query");
        }
        case USERS_COUNT, USERS_SUMMARY, USERS_STATS -> {
            if (asString(args.get("criteriaText")).isBlank())
                errors.add("step[" + index + "] " + tool + " requires criteriaText");
        }
        case USERS_RECENT -> {
            Integer limit = asInt(args.get("limit"));
            if (limit == null || limit < 1)
                errors.add("step[" + index + "] USERS_RECENT requires limit");
        }
        }
        return errors;
    }

    private String asString(Object v) {
        return (v == null) ? "" : String.valueOf(v).trim();
    }

    private Integer asInt(Object v) {
        if (v instanceof Integer i)
            return i;
        if (v instanceof Number n)
            return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // ==========================================
    // 2. Structured Output을 위한 DTO
    // ==========================================
    @Data
    @NoArgsConstructor
    public static class AiPlanDraft {
        private List<Step> steps = new ArrayList<>();

        @Data
        @NoArgsConstructor
        public static class Step {
            private String tool;
            private Map<String, Object> args;
        }
    }

    public record ValidationResult(boolean valid, List<String> errors, Plan plan) {}
}
