package com.aipa.runtime.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeEngineClientTest {

    private MockWebServer mockWebServer;
    private KnowledgeEngineClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        client = new KnowledgeEngineClient(baseUrl.substring(0, baseUrl.length() - 1));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void searchShouldCallKnowledgeEngineSearchEndpoint() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"k1\",\"title\":\"付款流程\",\"category\":\"API\"}]"));

        List<Map<String, Object>> result = client.search("demo", "付款流程", 3);

        assertEquals(1, result.size());
        assertEquals("付款流程", result.get(0).get("title"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/engine/knowledge/search", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"project_id\":\"demo\""));
        assertTrue(body.contains("\"query\":\"付款流程\""));
        assertTrue(body.contains("\"top_k\":3"));
    }

    @Test
    void listItemsShouldPassProjectAndCategoryQueryParams() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"k2\",\"title\":\"OrderController\",\"category\":\"ARCHITECTURE\"}]"));

        List<Map<String, Object>> result = client.listItems("demo", "ARCHITECTURE");

        assertEquals(1, result.size());
        assertEquals("OrderController", result.get(0).get("title"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertNotNull(request.getPath());
        assertTrue(request.getPath().startsWith("/engine/knowledge/items?"));
        assertTrue(request.getPath().contains("project_id=demo"));
        assertTrue(request.getPath().contains("category=ARCHITECTURE"));
    }
}


