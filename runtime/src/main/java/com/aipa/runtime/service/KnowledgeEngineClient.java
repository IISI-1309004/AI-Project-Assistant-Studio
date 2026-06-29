package com.aipa.runtime.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class KnowledgeEngineClient {

    private final RestClient restClient;

    public KnowledgeEngineClient(@Value("${aipa.ai-engine-url}") String aiEngineUrl) {
        this.restClient = RestClient.builder().baseUrl(aiEngineUrl).build();
    }

    public Map<String, Object> bulkIngest(String projectId, Map<String, Object> scanResult) {
        Map<String, Object> request = Map.of(
                "project_id", projectId,
                "scan_result", scanResult
        );
        return restClient.post()
                .uri("/engine/knowledge/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    public List<Map<String, Object>> search(String projectId, String query, int topK) {
        Map<String, Object> request = Map.of(
                "project_id", projectId,
                "query", query,
                "top_k", topK
        );
        return restClient.post()
                .uri("/engine/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(List.class);
    }

    public List<Map<String, Object>> listItems(String projectId, String category) {
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/engine/knowledge/items").queryParam("project_id", projectId);
                    if (category != null && !category.isBlank()) {
                        uriBuilder.queryParam("category", category);
                    }
                    return uriBuilder.build();
                });

        return request.retrieve().body(List.class);
    }
}

