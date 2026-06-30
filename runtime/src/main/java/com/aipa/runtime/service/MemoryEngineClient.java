package com.aipa.runtime.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class MemoryEngineClient {

    private final RestClient restClient;

    public MemoryEngineClient(@Value("${aipa.ai-engine-url}") String aiEngineUrl) {
        this.restClient = RestClient.builder().baseUrl(aiEngineUrl).build();
    }

    public Map<String, Object> getContext(String projectId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/engine/memory/context").queryParam("project_id", projectId).build())
                .retrieve()
                .body(Map.class);
    }

    public List<Map<String, Object>> query(String projectId, String type) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/engine/memory/query");
                    if (projectId != null && !projectId.isBlank()) {
                        uriBuilder.queryParam("project_id", projectId);
                    }
                    if (type != null && !type.isBlank()) {
                        uriBuilder.queryParam("type", type);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(List.class);
    }

    public Map<String, Object> reinforce(String memoryId) {
        return restClient.post()
                .uri("/engine/memory/reinforce/{memoryId}", memoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> getById(String memoryId) {
        return restClient.get()
                .uri("/engine/memory/item/{memoryId}", memoryId)
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> getItem(String memoryId) {
        try {
            return restClient.get()
                    .uri("/engine/memory/items/{memoryId}", memoryId)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}

