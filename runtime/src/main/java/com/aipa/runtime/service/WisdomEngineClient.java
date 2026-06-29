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
 * WisdomEngineClient — Phase 6
 * 呼叫 AI Engine 的 Wisdom Engine REST API
 */
@Service
public class WisdomEngineClient {

    private static final Logger log = LoggerFactory.getLogger(WisdomEngineClient.class);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WisdomEngineClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    /**
     * 對程式碼變更進行規則匹配，回傳命中的智慧規則
     */
    public List<Map<String, Object>> matchRules(Map<String, Object> context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/engine/wisdom/match"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("WisdomEngineClient.matchRules failed: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * 列出智慧規則
     */
    public List<Map<String, Object>> listRules(String projectId) {
        try {
            String url = baseUrl + "/engine/wisdom/rules?enabled_only=true";
            if (projectId != null && !projectId.isBlank()) {
                url += "&project_id=" + projectId;
            }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("WisdomEngineClient.listRules failed: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * 新增智慧規則
     */
    public Map<String, Object> addRule(Map<String, Object> rule) {
        try {
            String json = objectMapper.writeValueAsString(rule);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/engine/wisdom/rules"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("WisdomEngineClient.addRule failed: {}", e.getMessage());
        }
        return Map.of("error", "WisdomEngine unavailable");
    }

    /**
     * 快速檢查是否有 BLOCK 規則命中
     */
    public boolean hasBlockViolation(Map<String, Object> context) {
        List<Map<String, Object>> matched = matchRules(context);
        return matched.stream().anyMatch(r -> "BLOCK".equals(r.get("severity")));
    }
}

