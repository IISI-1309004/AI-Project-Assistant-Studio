package com.aipa.runtime.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningEngineClientTest {

    private MockWebServer mockWebServer;
    private LearningEngineClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        client = new LearningEngineClient(baseUrl.substring(0, baseUrl.length() - 1));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void analyzeShouldCallLearningAnalyzeEndpoint() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"learning_id\":\"l-1\",\"new_knowledge_count\":1,\"new_memory_count\":1}"));

        Map<String, Object> result = client.analyze(Map.of("project_id", "demo", "summary", "merged"));
        assertEquals("l-1", result.get("learning_id"));

        RecordedRequest req = mockWebServer.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/engine/learning/analyze", req.getPath());
        assertTrue(req.getBody().readUtf8().contains("\"project_id\":\"demo\""));
    }

    @Test
    void getResultAndRollbackShouldCallExpectedEndpoints() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"learning_id\":\"l-1\",\"status\":\"COMPLETED\"}"));
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"learning_id\":\"l-1\",\"message\":\"Learning rollback completed\"}"));

        Map<String, Object> result = client.getResult("l-1");
        Map<String, Object> rollback = client.rollback("l-1");

        assertEquals("COMPLETED", result.get("status"));
        assertNotNull(rollback.get("message"));

        RecordedRequest getReq = mockWebServer.takeRequest();
        assertEquals("GET", getReq.getMethod());
        assertEquals("/engine/learning/result/l-1", getReq.getPath());

        RecordedRequest rollbackReq = mockWebServer.takeRequest();
        assertEquals("POST", rollbackReq.getMethod());
        assertEquals("/engine/learning/rollback/l-1", rollbackReq.getPath());
    }
}

