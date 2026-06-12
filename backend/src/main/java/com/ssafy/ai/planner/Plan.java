package com.ssafy.ai.planner;

import java.util.ArrayList;
import java.util.List;

public class Plan {

    private final List<PlanStep> steps = new ArrayList<>();

    public List<PlanStep> getSteps() {
        return steps;
    }

    public Plan add(PlanStep step) {
        steps.add(step);
        return this;
    }
}
