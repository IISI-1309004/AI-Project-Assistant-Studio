package com.aipa.intellij.listener

import com.aipa.intellij.api.AipaApiClient
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Checkpoint 輪詢服務
 * 定期檢查是否有新的 Checkpoint，觸發通知
 */
@Service(Service.Level.PROJECT)
class CheckpointPollingService(private val project: Project) {
    private val seenCheckpointIds = ConcurrentHashMap<String, Boolean>()
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    init {
        startPolling()
    }

    private fun startPolling() {
        executor.scheduleAtFixedRate(
            { pollCheckpoints() },
            10,
            10,
            TimeUnit.SECONDS
        )
    }

    private fun pollCheckpoints() {
        try {
            val apiClient = AipaApiClient.getInstance()
            val checkpoints = apiClient.listCheckpoints()

            for (checkpoint in checkpoints) {
                val id = checkpoint.checkpointId
                if (!seenCheckpointIds.containsKey(id)) {
                    seenCheckpointIds[id] = true
                    notifyCheckpoint(checkpoint)
                }
            }
        } catch (e: Exception) {
            // 靜默忽略連線錯誤
        }
    }

    private fun notifyCheckpoint(checkpoint: com.aipa.intellij.api.CheckpointItem) {
        val typeDisplay = checkpoint.type ?: "檢查點"
        val message = "AIPA 檢查點待審核：$typeDisplay"

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("AIPA Studio")
            ?.createNotification(
                "AIPA 檢查點",
                message,
                NotificationType.INFORMATION
            )

        notification?.notify(project)
    }

    fun dispose() {
        executor.shutdown()
    }
}
