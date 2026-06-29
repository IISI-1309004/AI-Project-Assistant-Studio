package com.aipa.workflow.planning;

import com.aipa.workflow.confidence.ConfidenceScore;
import com.aipa.workflow.spec.Specification;

import java.util.Map;

/**
 * PlanningEngine — 任務分解引擎介面
 */
public interface PlanningEngine {
    Map<String, Object> createTaskPlan(Specification spec, ConfidenceScore confidenceScore);
}

