package com.aipa.runtime.config;

import com.aipa.workflow.confidence.ConfidenceEngine;
import com.aipa.workflow.confidence.ConfidenceEngineImpl;
import com.aipa.workflow.planning.PlanningEngine;
import com.aipa.workflow.planning.PlanningEngineImpl;
import com.aipa.workflow.spec.SpecEngine;
import com.aipa.workflow.spec.SpecEngineImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WorkflowEngineConfig — 工作流引擎配置
 * 顯式註冊來自 workflow 模組的引擎 beans，以支援單模組構建和 IDE 識別。
 */
@Configuration
public class WorkflowEngineConfig {

    @Bean
    @ConditionalOnMissingBean(SpecEngine.class)
    public SpecEngine specEngine() {
        return new SpecEngineImpl();
    }

    @Bean
    @ConditionalOnMissingBean(ConfidenceEngine.class)
    public ConfidenceEngine confidenceEngine() {
        return new ConfidenceEngineImpl();
    }

    @Bean
    @ConditionalOnMissingBean(PlanningEngine.class)
    public PlanningEngine planningEngine() {
        return new PlanningEngineImpl();
    }
}

