package com.aipa.intellij.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * AskAipaAction — Phase 1 骨架（Phase 7 實作完整功能）
 */
class AskAipaAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AIPA Studio")
            ?.createNotification(
                "AIPA Studio",
                "Phase 7 will implement full Ask AIPA functionality",
                NotificationType.INFORMATION
            )
            ?.notify(project)
    }
}
