# AIPA Studio — 架構鎖定宣告（Architecture Lock）

**宣告日期**：Phase 1 實作開始  
**版本**：1.0.0  
**狀態**：🔒 已鎖定

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

## 當前 Phase

**Phase 1 — 全流程骨架**（進行中）

目標：建立所有模組的程式碼骨架，確保建構系統正常、CI 流水線通過、各服務可健康啟動。此階段所有引擎方法回傳空值或 `NotImplementedException`，不包含業務邏輯。
