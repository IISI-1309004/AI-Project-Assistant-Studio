package com.aipa.runtime.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class LearningEngineClient {

    private final RestClient restClient;

    public LearningEngineClient(@Value("${aipa.ai-engine-url}") String aiEngineUrl) {
        this.restClient = RestClient.builder().baseUrl(aiEngineUrl).build();
    }

    public Map<String, Object> analyze(Map<String, Object> payload) {
        return restClient.post()
                .uri("/engine/learning/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> getResult(String learningId) {
        return restClient.get()
                .uri("/engine/learning/result/{learningId}", learningId)
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> rollback(String learningId) {
        return restClient.post()
                .uri("/engine/learning/rollback/{learningId}", learningId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(Map.class);
    }
}

