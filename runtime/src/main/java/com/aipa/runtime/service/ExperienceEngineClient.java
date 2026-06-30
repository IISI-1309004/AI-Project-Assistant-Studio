package com.aipa.runtime.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * ExperienceEngineClient — Phase 6
 * 呼叫 AI Engine 的 Experience Engine REST API
 */
@Service
public class ExperienceEngineClient {

    private static final Logger log = LoggerFactory.getLogger(ExperienceEngineClient.class);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExperienceEngineClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    /**
     * 語意搜尋相似 ExperienceCase（只回傳相似度 > 0.6）
     */
    public List<Map<String, Object>> searchSimilar(String query, String projectId, int topK) {
        try {
            Map<String, Object> body = Map.of(
                    "query", query,
                    "project_id", projectId,
                    "top_k", topK
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/engine/experience/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("ExperienceEngineClient.searchSimilar failed: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * 建立 ExperienceCase（Session 完成後呼叫）
     */
    public Map<String, Object> createCase(Map<String, Object> caseData) {
        try {
            String json = objectMapper.writeValueAsString(caseData);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/engine/experience/cases"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<>() {});
            }
            log.warn("ExperienceEngineClient.createCase returned {}: {}", resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.warn("ExperienceEngineClient.createCase failed: {}", e.getMessage());
        }
        return Map.of("error", "ExperienceEngine unavailable");
    }

    /**
     * 列出指定專案的 ExperienceCases
     */
    public List<Map<String, Object>> listCases(String projectId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/engine/experience/cases?project_id=" + projectId))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("ExperienceEngineClient.listCases failed: {}", e.getMessage());
        }
        return List.of();
    }
}

