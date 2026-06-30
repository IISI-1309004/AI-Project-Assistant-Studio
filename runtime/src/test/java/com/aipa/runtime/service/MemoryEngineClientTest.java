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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryEngineClientTest {

    private MockWebServer mockWebServer;
    private MemoryEngineClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        client = new MemoryEngineClient(baseUrl.substring(0, baseUrl.length() - 1));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void queryShouldPassParams() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"m1\",\"type\":\"PATTERN\",\"key\":\"layered\"}]"));

        List<Map<String, Object>> result = client.query("demo", "PATTERN");
        assertEquals(1, result.size());

        RecordedRequest req = mockWebServer.takeRequest();
        assertEquals("GET", req.getMethod());
        assertTrue(req.getPath().contains("project_id=demo"));
        assertTrue(req.getPath().contains("type=PATTERN"));
    }

    @Test
    void getByIdAndReinforceShouldCallExpectedEndpoints() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"m1\",\"type\":\"PATTERN\"}"));
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"m1\",\"strength\":7}"));

        client.getById("m1");
        client.reinforce("m1");

        RecordedRequest getReq = mockWebServer.takeRequest();
        assertEquals("GET", getReq.getMethod());
        assertEquals("/engine/memory/item/m1", getReq.getPath());

        RecordedRequest reinforceReq = mockWebServer.takeRequest();
        assertEquals("POST", reinforceReq.getMethod());
        assertEquals("/engine/memory/reinforce/m1", reinforceReq.getPath());
    }
}

