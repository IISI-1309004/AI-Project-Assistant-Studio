package com.aipa.intellij.tool

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel

/**
 * AIPA 工具視窗工廠
 * 負責在 IDE 中建立側邊欄工具視窗
 */
class AipaToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolPanel = AipaToolPanel(project)
        val content = ContentFactory.getInstance().createContent(
            toolPanel,
            "AIPA 工作階段",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}

/**
 * AIPA 工具視窗面板
 * 負責顯示工作階段、檢查點、操作按鈕等
 */
class AipaToolPanel(project: Project) : JPanel() {
    init {
        // Phase 7 MVP：簡易的 Swing 面板
        // 生產版本可考慮 JCEF (Java Chromium Embedded Framework) 嵌入 React UI

        val label = javax.swing.JLabel("AIPA 工具視窗 — Phase 7 實作中")
        label.font = label.font.deriveFont(12f)
        add(label)
    }
}

