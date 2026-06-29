package com.aipa.intellij.actions

import com.aipa.intellij.api.AipaApiClient
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages

/**
 * 詢問 AIPA 動作 — Phase 7 完整實作
 * 允許使用者在 IDE 中向 AIPA 提出功能需求
 */
class AskAipaAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 取得編輯器選取的文字
        val selectedText = getSelectedText(e)

        // 顯示輸入對話方塊
        val requirement = Messages.showInputDialog(
            project,
            "請描述您希望 AIPA 建置的功能",
            "詢問 AIPA",
            Messages.getQuestionIcon(),
            selectedText,
            null
        ) ?: return

        if (requirement.isBlank()) return

        // 非同步執行請求
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                try {
                    val apiClient = AipaApiClient.getInstance()
                    val session = apiClient.createSession(requirement, "intellij-plugin")

                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("AIPA Studio")
                        ?.createNotification(
                            "AIPA 工作階段已建立",
                            "工作階段 ID：${session.sessionId}",
                            NotificationType.INFORMATION
                        )
                        ?.notify(project)
                } catch (ex: Exception) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("AIPA Studio")
                        ?.createNotification(
                            "AIPA 詢問失敗",
                            "錯誤：${ex.message}",
                            NotificationType.ERROR
                        )
                        ?.notify(project)
                }
            },
            "向 AIPA 詢問...",
            true,
            project
        )
    }

    private fun getSelectedText(e: AnActionEvent): String {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return ""
        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            selectionModel.selectedText ?: ""
        } else {
            ""
        }
    }
}
