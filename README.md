# AIPA Studio

> **AI Project Assistant Studio** — 企業級 AI 開發平台

[![Phase](https://img.shields.io/badge/Phase-9%20企業強化-green)](docs/roadmap.md)
[![Architecture Lock](https://img.shields.io/badge/Architecture-Locked-red)](docs/ARCHITECTURE-LOCK.md)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-27%2F27%20Passing-brightgreen)](#)

---

## 什麼是 AIPA Studio？

AIPA Studio 是一套可安裝於企業內部的 AI 開發平台。它不是 ChatBot、不是 Prompt 工具、不是 Copilot Plugin。

它是一個能整合 Claude、OpenAI、Gemini、Ollama 等 AI 的 **Enterprise AI Development Platform**，讓開發人員只需輸入一句需求，系統即可自動完成：分析 → 設計 → Coding → Testing → Review → Learning。

> 🔒 所有企業資料留在企業內部，不傳送至任何第三方伺服器。

## 📚 文件導航

| 文件 | 說明 |
|------|------|
| 🚀 **[安裝手冊](docs/installation-guide.md)** | Docker/Linux/Windows 詳細安裝步驟 |
| 📖 **[使用手冊](docs/user-guide.md)** | CLI 指令完整參考、Web Dashboard、IDE 外掛 |
| 📋 **[Phase Gate 追蹤](docs/phase-gate-tracker.md)** | 各 Phase 進入/退出標準與實際完成狀態 |
| [系統架構（SAD）](docs/sad.md) | 系統架構設計 |
| [開發路線圖](docs/roadmap.md) | Phase 0 → v1.0.0 GA |

## 核心理念：LSDD

**Learning Spec Driven Development（學習規格驅動開發）**

- 🧠 **Knowledge First** — 所有決策由知識庫驅動
- 💾 **Memory First** — 持久記憶，不再重複輸入上下文
- 📋 **Specification First** — 規格核准後才開始 Coding
- 🔒 **Human Checkpoint** — 四道強制人工審核關卡
- 📈 **Learning Always** — 每次 PR Merge 後 AI 自動成長

## 🚀 快速開始（3 分鐘，Windows）

### 1. 自動安裝（推薦）

在 **PowerShell**（以管理員身份）執行：

```powershell
cd AI-Project-Assistant-Studio\installer\windows
Set-ExecutionPolicy Bypass -Scope Process -Force
.\install.ps1
```

完成後重新開啟 PowerShell：

```powershell
aipa doctor  # 驗證安裝狀態
```

### 2. 初始化你的 Java 專案並提出需求

```powershell
cd C:\your\java\project
aipa init                                     # 掃描專案，建立知識庫（3-10 分鐘）
aipa ask "新增客戶付款到期前三天提醒功能"    # 提出需求，開始工作流程
aipa checkpoint list                          # 查看待審核 Checkpoint
aipa checkpoint approve <checkpointId>        # 核准後繼續
```

➡️ 詳細說明請見 **[使用手冊](docs/user-guide.md)**

## 架構概覽

```
開發人員（CLI / Web Dashboard / VSCode / IntelliJ）
         │
         ▼
  Runtime Service（Java/Spring Boot :18080）
         │
         ├── Scanner Engine（掃描 Java/SQL/API）
         ├── Spec Engine（規格生成）
         ├── Confidence Engine（信心評估，70% 關卡）
         ├── Planning Engine（任務分解）
         ├── AI Adapter（Claude/OpenAI/Gemini/Ollama）
         ├── Testing Engine（自動測試）
         ├── Review Engine（架構/安全/SQL 審查）
         └── AI Engine（Python/FastAPI :18082）
               ├── Knowledge Engine（語意搜尋）
               ├── Memory Engine（持久記憶）
               ├── Learning Engine（PR 後學習）
               ├── Experience Engine（相似案例）
               └── Wisdom Engine（企業規則）
```

## ✅ 功能狀態

| Phase | 功能 | 狀態 |
|-------|------|------|
| Phase 1–3 | 核心 MVP（掃描、規格、Human Checkpoint） | ✅ 完成 |
| Phase 4 | AI 介面卡 + 完整工作流程 | ✅ 完成 |
| Phase 5 | 學習引擎 + 記憶引擎 | ✅ 完成 |
| Phase 6 | 經驗引擎 + 智慧規則引擎 | ✅ 完成 |
| Phase 7 | VSCode/IntelliJ 外掛 + Web Dashboard | ✅ 完成 |
| Phase 8 | Docker/Linux/Windows 安裝器 | ✅ 完成 |
| Phase 9 | IP 白名單、JSON 日誌、RBAC | ✅ 完成 |
| v1.0.0 GA | UAT + 效能測試 + 安全掃描 | 📋 待辦 |

## 授權

Apache License 2.0
