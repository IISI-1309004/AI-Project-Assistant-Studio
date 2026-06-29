package com.aipa.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.aipa.intellij.listener.CheckpointPollingService

/**
 * AIPA Plugin 專案監聽器
 * 負責在專案開啟/關閉時初始化/清理服務
 */
class AipaProjectListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        // 初始化 Checkpoint 輪詢服務
        val pollingService = project.getService(CheckpointPollingService::class.java)
        // 服務會自動啟動
    }

    override fun projectClosed(project: Project) {
        // 清理資源
        val pollingService = project.getService(CheckpointPollingService::class.java)
        pollingService.dispose()
    }
}

