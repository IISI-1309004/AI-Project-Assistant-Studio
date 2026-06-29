# AIPA Studio

> **AI Project Assistant Studio** — 企業級 AI 開發平台

[![Phase](https://img.shields.io/badge/Phase-1%20骨架-blue)](docs/roadmap.md)
[![Architecture Lock](https://img.shields.io/badge/Architecture-Locked-red)](docs/ARCHITECTURE-LOCK.md)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](LICENSE)

---

## 什麼是 AIPA Studio？

AIPA Studio 是一套可安裝於企業內部的 AI 開發平台。它不是 ChatBot、不是 Prompt 工具、不是 Copilot Plugin。

它是一個能整合 GitHub Copilot、Claude Code、Gemini CLI 等 AI Agent 的 **Enterprise AI Development Platform**，讓開發人員只需輸入一句需求，系統即可自動完成：分析 → 設計 → Coding → Testing → Review → Learning。

## 核心理念：LSDD

**Learning Spec Driven Development（學習規格驅動開發）**

- 🧠 **Knowledge First** — 所有決策由知識庫驅動
- 💾 **Memory First** — 持久記憶，不再重複輸入上下文
- 📋 **Specification First** — 規格核准後才開始 Coding
- 🔒 **Human Checkpoint** — 四道強制人工審核關卡
- 📈 **Learning Always** — 每次 PR Merge 後 AI 自動成長

## 快速開始

### 方式一：Docker Compose（推薦）

```bash
cp installer/docker/.env.example installer/docker/.env
# 編輯 .env 填入 AI 供應商 API Key
docker compose -f installer/docker/docker-compose.yml up -d
aipa init
```

### 方式二：從原始碼建構

```bash
# 前置需求：Java 17、Node.js 20、Python 3.11、Poetry
make build
make docker-up
aipa init
```

## 文件

| 文件 | 說明 |
|---|---|
| [產品願景](docs/vision.md) | 為什麼建立 AIPA Studio |
| [產品需求（PRD）](docs/prd.md) | 完整功能需求規格 |
| [系統架構（SAD）](docs/sad.md) | 系統架構設計 |
| [領域模型](docs/domain-model.md) | 核心領域實體與業務規則 |
| [模組設計](docs/module-design.md) | 18 個模組的詳細設計 |
| [開發路線圖](docs/roadmap.md) | Phase 0 → v1.0.0 GA |

## 架構概覽

```
開發人員輸入一句需求
        ↓
  AIPA Runtime Service（Spring Boot :18080）
        ↓
  Spec Engine → Confidence Engine → Planning Engine
        ↓
  AI Adapter（Copilot / Claude / Gemini / Ollama）
        ↓
  Testing Engine → Review Engine
        ↓
  Human Checkpoint → Git PR → Merge
        ↓
  Learning Engine（知識庫自動更新）
```

## 目前狀態

**Phase 1 — 全流程骨架**（進行中）

所有模組骨架建立中，尚無業務邏輯。

## 授權

Apache License 2.0
