package com.aipa.intellij.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * AIPA 會話資訊
 */
data class SessionItem(
    val sessionId: String,
    val status: String,
    val requirement: String? = null
)

/**
 * AIPA 檢查點資訊
 */
data class CheckpointItem(
    val checkpointId: String,
    val sessionId: String? = null,
    val type: String? = null,
    val status: String? = null
)

/**
 * AIPA API 客戶端 — HTTP 代理
 * 負責與 Runtime 服務通訊
 */
@Service
class AipaApiClient {
    private val httpClient = HttpClient.newBuilder().build()

    private fun getBaseUrl(): String {
        return System.getProperty("aipa.runtime.url", "http://localhost:8080")
            .trimEnd('/')
    }

    fun listSessions(): List<SessionItem> {
        val url = "${getBaseUrl()}/api/v1/session"
        val response = executeGet(url)
        // 簡易解析（實際環境應使用 JSON 庫）
        return emptyList()
    }

    fun listCheckpoints(): List<CheckpointItem> {
        val url = "${getBaseUrl()}/api/v1/checkpoint"
        val response = executeGet(url)
        return emptyList()
    }

    fun createSession(requirement: String, projectId: String): SessionItem {
        val url = "${getBaseUrl()}/api/v1/session"
        val json = """{"requirement":"$requirement","projectId":"$projectId","projectRoot":"${System.getProperty("user.dir")}"}"""
        val response = executePost(url, json)
        // 暫時回傳示意物件
        return SessionItem(
            sessionId = "session-${System.currentTimeMillis()}",
            status = "進行中"
        )
    }

    fun approveCheckpoint(checkpointId: String) {
        val url = "${getBaseUrl()}/api/v1/checkpoint/$checkpointId/approve"
        val json = """{"reviewer":"intellij-plugin","comment":"已從 IntelliJ 外掛程式核准"}"""
        executePost(url, json)
    }

    fun rejectCheckpoint(checkpointId: String) {
        val url = "${getBaseUrl()}/api/v1/checkpoint/$checkpointId/reject"
        val json = """{"reviewer":"intellij-plugin","comment":"已從 IntelliJ 外掛程式拒絕"}"""
        executePost(url, json)
    }

    private fun executeGet(url: String): String {
        val request = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP ${response.statusCode()} from $url")
        }
        return response.body()
    }

    private fun executePost(url: String, jsonBody: String): String {
        val request = HttpRequest.newBuilder(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .header("Content-Type", "application/json")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP ${response.statusCode()} from $url")
        }
        return response.body()
    }

    companion object {
        fun getInstance(): AipaApiClient =
            ApplicationManager.getApplication().getService(AipaApiClient::class.java)
    }
}

