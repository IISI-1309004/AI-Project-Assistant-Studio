# AIPA Studio — 架構鎖定宣告（Architecture Lock）

**宣告日期**：Phase 1 實作開始
**版本**：1.0.0
**狀態**：🔒 已鎖定
**最後更新**：2026-06-30

---

## 架構鎖定宣告

本文件宣告 AIPA Studio 的 Phase 0 設計階段正式結束，架構鎖定生效。

從即日起，所有程式碼實作必須嚴格遵循以下設計文件中定義的架構、模組邊界與技術選型。

## 已鎖定的設計文件

| 文件 | 路徑 | 狀態 |
|---|---|---|
| 產品願景文件 | `docs/vision.md` | 🔒 鎖定 |
| 產品需求文件 | `docs/prd.md` | 🔒 鎖定 |
| 系統架構文件 | `docs/sad.md` | 🔒 鎖定 |
| 領域模型 | `docs/domain-model.md` | 🔒 鎖定 |
| 模組設計 | `docs/module-design.md` | 🔒 鎖定 |
| 循序圖 | `docs/sequence-diagrams.md` | 🔒 鎖定 |
| 類別圖 | `docs/class-diagrams.md` | 🔒 鎖定 |
| 部署圖 | `docs/deployment-diagram.md` | 🔒 鎖定 |
| Repository 結構 | `docs/repository-structure.md` | 🔒 鎖定 |
| 技術選型 | `docs/technology-selection.md` | 🔒 鎖定 |
| 開發路線圖 | `docs/roadmap.md` | 🔒 鎖定 |

## 架構鎖定規則（強制執行）

1. **Phase 順序不可跳躍** — 每個 Phase 的退出標準全部滿足後，才可開始下一個 Phase
2. **架構變更需 ADR** — 任何超出設計文件範圍的架構決策，必須先建立 ADR 並通過審查
3. **技術不可擅自引入** — 新增技術前必須更新 `docs/technology-selection.md` 並審查
4. **介面契約向下相容** — 模組公開 API 一旦在 Phase 1 定義，後續 Phase 不得進行破壞性變更
5. **Human Checkpoint 永不可繞過** — 四個 Checkpoint 關卡（Spec/Impact/Task/PR Approval）永遠強制執行

## 允許在不重新審查的情況下變更的內容

- 方法內部實作邏輯（不影響公開 API）
- 單元測試新增或修改
- 文件錯字修正
- 設定檔預設值調整（不影響功能行為）

## 當前 Phase 進度

**Phase 9 — 企業強化（已完成）**

所有 Phase 1–9 均已實作完成。系統正準備 v1.0.0 正式發布。

| Phase | 狀態 | 主要交付物 |
|-------|------|-----------|
| Phase 0 — 架構鎖定 | 🔒 完成 | 11 份設計文件 |
| Phase 1 — 全流程骨架 | ✅ 完成 | 所有模組骨架 |
| Phase 2 — 核心流水線 | ✅ 完成 | Scanner + Knowledge Engine |
| Phase 3 — 規格 + Checkpoint | ✅ 完成 | SpecEngine + MVP 達成 |
| Phase 4 — AI 介面卡 | ✅ 完成 | AI Adapters + Review Engine |
| Phase 5 — 學習 + 記憶 | ✅ 完成 | Learning + Memory Engine |
| Phase 6 — 經驗 + 智慧 | ✅ 完成 | Experience + Wisdom Engine |
| Phase 7 — Plugin 套件 | ✅ 完成 | VSCode + IntelliJ + Web UI |
| Phase 8 — Installer | ✅ 完成 | Docker/Linux/Windows 腳本 |
| Phase 9 — 企業強化 | ✅ 完成 | IP 白名單/JSON 日誌/RBAC |
| v1.0.0 GA | 📋 待辦 | UAT + 效能測試 + 安全掃描 |

詳細的每 Phase 進入/退出標準與實際完成狀態，請參見：
👉 `docs/phase-gate-tracker.md`

