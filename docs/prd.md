# AIPA Studio — 產品需求文件（PRD）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 產品團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：[產品願景文件](./vision.md)

---

## 1. 使用者角色（User Personas）

### 1.1 企業開發人員（Enterprise Developer）

| 項目 | 描述 |
|---|---|
| **角色** | 日常功能開發、Bug 修復、維護工作的執行者 |
| **技術能力** | 熟悉 Java / Spring Boot，具備基本 Git 操作能力 |
| **主要痛點** | 每次 AI Session 都需要重新說明專案背景；AI 生成的程式碼常違反架構規則；不知道歷史 PR 的決策原因 |
| **期望** | 輸入一句需求，得到一個符合架構、業務規則且附有測試的 PR |
| **主要使用命令** | `aipa ask`、`aipa init`、`aipa status` |

### 1.2 技術負責人（Tech Lead）

| 項目 | 描述 |
|---|---|
| **角色** | 負責技術決策、架構守護、PR Review、新人指導 |
| **技術能力** | 深度理解系統架構，具備跨模組影響分析能力 |
| **主要痛點** | 每個 PR 都要重複解釋架構規則；新人不了解業務規則；架構決策沒有被記錄 |
| **期望** | 系統自動守護架構規則；架構決策自動被記憶；PR Review 前已完成自動化審查 |
| **主要使用命令** | `aipa review`、`aipa wisdom add`、`aipa learn`、`aipa checkpoint` |

### 1.3 系統架構師（Architect）

| 項目 | 描述 |
|---|---|
| **角色** | 定義系統架構、技術選型、ADR 記錄、長期技術規劃 |
| **技術能力** | 全棧架構視野，熟悉企業級設計模式與非功能性需求 |
| **主要痛點** | 架構決策（ADR）沒有被 AI 遵守；影響分析依賴人工；缺乏系統性知識管理 |
| **期望** | ADR 自動被 Knowledge Engine 吸收；所有 AI 行為遵守架構決策；影響分析自動化 |
| **主要使用命令** | `aipa knowledge add`、`aipa adr`、`aipa impact` |

### 1.4 DevOps 工程師（DevOps Engineer）

| 項目 | 描述 |
|---|---|
| **角色** | 負責 CI/CD 流水線、環境管理、AIPA Studio 平台維運 |
| **技術能力** | 熟悉 Docker、Linux、Git、CI/CD 工具鏈 |
| **主要痛點** | AI 工具整合複雜；企業安全要求與雲端 SaaS 衝突；多專案管理困難 |
| **期望** | Docker Compose 一鍵部署；完全本地化；支援多專案管理；提供健康檢查 API |
| **主要使用命令** | `aipa server start`、`aipa server status`、`aipa project list` |

---

## 2. 使用者旅程（User Journeys）

### 2.1 旅程一：`aipa init` — 首次專案初始化

**角色**：企業開發人員  
**前提**：AIPA Studio 已安裝，使用者位於 Java/Spring Boot 專案根目錄

```
步驟 1：使用者執行 aipa init
步驟 2：CLI 連線至 AIPA Runtime Service（Port 18080）
步驟 3：Runtime 啟動 Scanner Engine
步驟 4：Scanner 自動偵測技術棧
         → Java 版本、Spring Boot 版本、MyBatis / Hibernate、Maven / Gradle
         → Oracle / PostgreSQL / SQL Server Schema
         → Vue / React / JSP 前端
         → OpenAPI / Swagger 規格
         → JBoss / WildFly / Docker 部署設定
步驟 5：Scanner 分析專案結構
         → 套件結構、Controller / Service / Repository 層次
         → API 端點清單
         → 資料庫 Table / Column / Index
         → 工作流程圖、呼叫圖、相依圖
步驟 6：Knowledge Engine 建立初始知識庫
         → 專案知識、API 知識、資料庫知識、架構知識
步驟 7：Memory Engine 建立初始記憶
         → Coding Style Memory、Architecture Memory
步驟 8：DNA Builder 建立 Project DNA
         → 分析 Log 模式、Exception 處理慣例
         → Transaction 邊界、Validation 模式
         → Mail、Scheduler、批次作業模式
步驟 9：建立 .ai-project/ 目錄結構
步驟 10：CLI 顯示初始化摘要報告
          → 偵測到的技術棧
          → 建立的知識項目數量
          → 識別的架構模式
          → 需要人工補充的資訊項目
```

**成功標準**：初始化完成後，`aipa ask` 可以在不輸入任何專案背景的情況下正確回答關於專案的問題。

---

### 2.2 旅程二：`aipa ask` — 完整 LSDD 開發週期（正常路徑）

**角色**：企業開發人員  
**前提**：`aipa init` 已完成，專案知識庫已建立

```
步驟 1：使用者執行 aipa ask "新增案件提醒功能"

步驟 2：Knowledge Engine 查詢
         → 查詢相關業務知識（案件管理模組、提醒機制）
         → 查詢相關 API 知識（現有 API 端點）
         → 查詢架構知識（Notification 模式）

步驟 3：Memory Engine 查詢
         → Session Memory（本次工作記憶）
         → Pattern Memory（相似功能開發模式）
         → Coding Style Memory（團隊 Coding 風格）
         → Decision Memory（相關歷史決策）

步驟 4：Experience Engine 查詢
         → 搜尋歷史相似功能（如：Email 通知、Push 通知）
         → 取得相似案例的成功與失敗模式

步驟 5：Specification Engine 生成 FeatureSpec
         → 需求分析
         → 知識上下文（引用 Knowledge + Memory + Experience）
         → 影響分析（受影響的模組、API、資料庫）
         → 風險評估（RiskLevel：LOW / MEDIUM / HIGH）
         → 回滾計劃
         → 測試計劃
         → 信心分數初評

【人工關卡 1：規格核准（Spec Approval）】
         → CLI 顯示完整規格摘要
         → Web UI 同步顯示（可詳細檢視）
         → IDE Plugin 發出通知
         → 開發人員審閱並輸入：approve / reject / edit
         → 若 reject 或 edit：調整規格後重新審閱

步驟 6：Confidence Engine 評估
         → 評估知識涵蓋率（0–100）
         → 若 < 70：觸發 NMI（Need More Information）
                    → 列出缺少的知識項目
                    → 等待使用者補充
         → 若 ≥ 70：繼續流程

步驟 7：Planning Engine 任務分解
         → 將規格分解為小任務
         → 每個任務：獨立、可測試、可追蹤
         → 建立任務相依圖
         → 每個任務附上信心分數

【人工關卡 2：任務核准（Task Approval）】
         → CLI 顯示任務清單與相依關係
         → 開發人員審閱並輸入：approve / reject / modify
         → 若修改：調整任務後重新審閱

步驟 8：逐任務執行（迴圈）
         步驟 8a：AI Adapter 呼叫
                  → 選擇已設定的 AI 供應商（Copilot / Claude / Gemini / Ollama）
                  → 傳送：任務規格 + 知識上下文 + 記憶 + 程式碼上下文
                  → 接收：生成的程式碼
         步驟 8b：Testing Engine
                  → 自動生成對應單元測試
                  → 執行測試
                  → 若測試失敗：回到 8a 修正
         步驟 8c：Review Engine
                  → 架構合規性審查
                  → Coding 規則審查
                  → 業務規則審查
                  → 安全性審查
                  → 效能審查
                  → SQL 審查（若涉及）
                  → API 審查（若涉及）
                  → Regression 風險審查
                  → 若審查不通過：回到 8a 修正
         步驟 8d：進入下一個任務

步驟 9：整體整合測試
         → Integration Test 執行
         → Regression Test 執行

【人工關卡 3：PR 核准（PR Approval）】
         → CLI 顯示 Code Diff 摘要、審查結果、測試結果
         → Web UI 顯示完整 Diff（可行內評論）
         → 開發人員審閱並輸入：approve / reject
         → 若 reject：列出修改意見，回到步驟 8

步驟 10：建立 Git PR
          → 自動生成 PR 標題（基於規格）
          → 自動生成 PR 描述（含規格摘要、影響分析、測試結果）
          → 推送至遠端

步驟 11：PR Merge 後觸發 Learning Engine
          → 見旅程四
```

---

### 2.3 旅程三：`aipa ask` — 信心不足路徑（NMI）

**角色**：企業開發人員

```
步驟 1–5：同旅程二

步驟 6：Confidence Engine 評估
         → 信心分數 = 45（< 70）
         → 觸發 NMI

步驟 6a：CLI 顯示 NMI 報告
          → 「信心分數：45 / 100，無法安全開始 Coding」
          → 缺少的知識項目清單：
            - 「付款模組與案件的關聯規則未知」
            - 「現有通知服務的 API 契約未掃描到」
            - 「提醒觸發的業務規則不明確」

步驟 6b：開發人員選擇補充方式
          選項 A：aipa knowledge add（手動補充知識）
          選項 B：aipa scan --target=notification（針對性重新掃描）
          選項 C：aipa ask --context="提醒規則是：..."（臨時補充上下文）

步驟 6c：補充後重新評估信心分數
          → 若 ≥ 70：繼續正常流程
          → 若仍 < 70：再次顯示 NMI
```

---

### 2.4 旅程四：PR Merge 後自動學習

**角色**：系統自動（由 Git Hook 或 CI 觸發）

```
觸發條件：PR 成功 Merge 至主分支

步驟 1：Learning Engine 接收 PR Merge 事件
         → PR ID、Merge Commit Hash、分支名稱

步驟 2：Git Diff 分析
         → 分析所有變更的檔案
         → 識別：新增類別、修改方法、新增 API、Schema 變更

步驟 3：Commit Message 分析
         → 提取功能描述、Bug 原因、重構理由

步驟 4：Reviewer Comment 分析
         → 提取架構建議、程式碼規則建議、業務規則說明

步驟 5：模式提取
         → 識別新的 Coding Pattern
         → 識別架構決策
         → 識別業務規則變更

步驟 6：知識庫更新
         → 新增或更新 KnowledgeItem
         → 更新 KnowledgeGraph 邊關係

步驟 7：記憶更新
         → 更新 PatternMemory
         → 更新 DecisionMemory
         → 更新 CodingStyleMemory

步驟 8：經驗庫更新
         → 將本次 Feature 加入 ExperienceLibrary
         → 標記為：成功案例 / 有風險案例

步驟 9：生成學習摘要報告
         → 「本次 PR 學習到 3 個新模式、更新 5 個知識項目」
         → 儲存於 Release Memory
```

---

## 3. 功能性需求

### 3.1 掃描引擎（Scanner Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| SCAN-001 | 自動偵測 Java 版本（8 / 11 / 17 / 21） | 必要 |
| SCAN-002 | 自動偵測 Spring Boot 版本與模組（MVC / Security / Data / Batch） | 必要 |
| SCAN-003 | 分析 MyBatis Mapper XML 與 Mapper Interface | 必要 |
| SCAN-004 | 分析 Hibernate Entity 類別與關聯關係 | 必要 |
| SCAN-005 | 分析 Oracle / PostgreSQL / SQL Server Schema（DDL） | 必要 |
| SCAN-006 | 分析 Maven pom.xml 或 Gradle build.gradle 相依關係 | 必要 |
| SCAN-007 | 分析 OpenAPI / Swagger 規格（yaml / json） | 必要 |
| SCAN-008 | 分析 Vue / React 元件結構 | 重要 |
| SCAN-009 | 建立 API 呼叫圖（Controller → Service → Repository） | 必要 |
| SCAN-010 | 建立類別相依圖 | 必要 |
| SCAN-011 | 分析 JavaDoc 作為業務知識來源 | 重要 |
| SCAN-012 | 分析 application.yml / application.properties | 必要 |
| SCAN-013 | 分析 JBoss / WildFly 部署描述符 | 重要 |
| SCAN-014 | 支援增量掃描（只掃描變更的檔案） | 重要 |
| SCAN-015 | 掃描結果儲存為結構化 Knowledge Items | 必要 |

### 3.2 知識引擎（Knowledge Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| KNOW-001 | 支援至少 8 種知識類別：專案、業務、架構、API、資料庫、工作流程、相依、規則 | 必要 |
| KNOW-002 | 每個知識項目含：類別、標題、內容、來源、建立時間、最後更新、信心分數 | 必要 |
| KNOW-003 | 支援語意搜尋（向量相似度搜尋） | 必要 |
| KNOW-004 | 支援關鍵字搜尋與語意搜尋的混合查詢 | 重要 |
| KNOW-005 | 知識項目可手動新增、編輯、刪除 | 必要 |
| KNOW-006 | 知識項目支援版本歷史（可查閱舊版本） | 重要 |
| KNOW-007 | 支援 ADR（Architecture Decision Record）格式儲存 | 必要 |
| KNOW-008 | 知識庫可匯出為 Markdown 格式 | 重要 |
| KNOW-009 | 知識圖譜顯示知識項目之間的關聯關係 | 重要 |
| KNOW-010 | 支援可選後端：SQLite（預設）/ PostgreSQL / Elasticsearch | 必要 |

### 3.3 記憶引擎（Memory Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| MEM-001 | Session Memory：記錄當前開發 Session 的所有決策與上下文 | 必要 |
| MEM-002 | Pattern Memory：記錄重複出現的程式碼模式與架構模式 | 必要 |
| MEM-003 | Decision Memory：記錄每個架構與設計決策及其理由 | 必要 |
| MEM-004 | Coding Style Memory：記錄團隊 Coding Style 規則 | 必要 |
| MEM-005 | Business Memory：記錄業務規則與業務術語定義 | 必要 |
| MEM-006 | Architecture Memory：記錄架構模式、分層規則、模組邊界 | 必要 |
| MEM-007 | Review Memory：記錄歷史 Code Review 意見與頻率 | 必要 |
| MEM-008 | Release Memory：記錄每個版本的變更摘要與學習結果 | 重要 |
| MEM-009 | 記憶跨 Session 持久化（重啟後不丟失） | 必要 |
| MEM-010 | 記憶可手動查閱、編輯 | 重要 |

### 3.4 學習引擎（Learning Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| LEARN-001 | 監聽 PR Merge 事件（Git Hook 或 CI Webhook） | 必要 |
| LEARN-002 | 自動分析 Git Diff，提取變更摘要 | 必要 |
| LEARN-003 | 分析 Commit Message 提取語意資訊 | 必要 |
| LEARN-004 | 分析 PR Reviewer Comment 提取規則與建議 | 必要 |
| LEARN-005 | 自動更新 Knowledge Engine（新增 / 修改知識項目） | 必要 |
| LEARN-006 | 自動更新 Memory Engine（更新 Pattern / Decision / Style Memory） | 必要 |
| LEARN-007 | 自動更新 Experience Engine（新增經驗案例） | 必要 |
| LEARN-008 | 生成學習摘要報告（每次 PR Merge 後） | 重要 |
| LEARN-009 | 支援手動觸發學習（`aipa learn --pr=<id>`） | 重要 |
| LEARN-010 | 學習結果可審閱與回滾（若學習結果有誤） | 重要 |

### 3.5 經驗引擎（Experience Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| EXP-001 | 建立經驗資料庫，按功能類型分類 | 必要 |
| EXP-002 | 記錄歷史 Feature、Bug Fix、Refactor 的完整上下文 | 必要 |
| EXP-003 | 支援相似案例語意搜尋 | 必要 |
| EXP-004 | 每個經驗案例含：成功指標、失敗指標、注意事項 | 必要 |
| EXP-005 | 在 Spec 生成時自動引用相似經驗 | 必要 |
| EXP-006 | 支援手動標記案例為「參考」或「避免」 | 重要 |

### 3.6 智慧引擎（Wisdom Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| WIS-001 | 支援手動新增智慧規則（`aipa wisdom add`） | 必要 |
| WIS-002 | 智慧規則含：規則描述、適用情境、嚴重程度（INFO / WARN / BLOCK） | 必要 |
| WIS-003 | BLOCK 級別規則：阻止 Coding 繼續，要求人工確認 | 必要 |
| WIS-004 | 在規格生成時自動套用相關智慧規則 | 必要 |
| WIS-005 | 在 Review 時自動套用智慧規則檢查 | 必要 |
| WIS-006 | 預設提供常見企業智慧規則範本 | 重要 |
| WIS-007 | 智慧規則可設定適用範圍（全域 / 特定模組 / 特定功能類型） | 重要 |

### 3.7 規格引擎（Specification Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| SPEC-001 | 支援四種規格類型：FeatureSpec / BugSpec / RefactorSpec / MigrationSpec | 必要 |
| SPEC-002 | 每份規格必須包含：需求、知識上下文、記憶引用、歷史、模式、決策、影響分析、風險、回滾、測試計劃、信心分數 | 必要 |
| SPEC-003 | 規格以 Markdown 格式儲存於 `specs/` 目錄 | 必要 |
| SPEC-004 | 支援規格模板（`templates/specs/`），可自訂 | 重要 |
| SPEC-005 | 規格需通過人工核准（Spec Approval）才能進入下一步 | 必要 |
| SPEC-006 | 規格版本化管理（每次修改保留版本歷史） | 重要 |
| SPEC-007 | 規格可匯出為 PDF / Confluence 格式 | 選用 |

### 3.8 規劃引擎（Planning Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| PLAN-001 | 自動將規格分解為小任務 | 必要 |
| PLAN-002 | 每個任務必須滿足：獨立、可測試、可追蹤三個條件 | 必要 |
| PLAN-003 | 建立任務相依圖（DAG，有向無環圖） | 必要 |
| PLAN-004 | 任務必須按相依順序執行（前置任務完成才能開始） | 必要 |
| PLAN-005 | 任務清單需通過人工核准（Task Approval）才能開始執行 | 必要 |
| PLAN-006 | 每個任務含估計的信心分數 | 重要 |
| PLAN-007 | 支援任務暫停、恢復、重做 | 重要 |
| PLAN-008 | 任務執行狀態即時更新（CLI / Web UI） | 重要 |

### 3.9 信心引擎（Confidence Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| CONF-001 | Coding 開始前必須評估信心分數（0–100） | 必要 |
| CONF-002 | 信心分數低於 70 時，強制阻止 Coding 並觸發 NMI | 必要 |
| CONF-003 | NMI 報告必須列出具體缺少的知識項目 | 必要 |
| CONF-004 | 信心評估考量：Knowledge 涵蓋率、Memory 完整性、Experience 相似度、架構複雜度、業務風險 | 必要 |
| CONF-005 | 信心閾值可設定（預設 70，可調整為 60–90） | 重要 |
| CONF-006 | 信心分數計算邏輯透明化（顯示各維度分數） | 重要 |

### 3.10 審查引擎（Review Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| REV-001 | 架構合規性審查（是否違反分層規則、模組邊界） | 必要 |
| REV-002 | Coding 規則審查（是否符合 Coding Style Memory） | 必要 |
| REV-003 | 業務規則審查（是否符合 Business Memory 中的業務規則） | 必要 |
| REV-004 | 安全性審查（SQL Injection、XSS、敏感資料洩漏） | 必要 |
| REV-005 | 效能審查（N+1 查詢、無索引查詢、記憶體洩漏風險） | 重要 |
| REV-006 | SQL 審查（缺少 WHERE 條件、全表掃描、缺少 Transaction） | 必要 |
| REV-007 | API 審查（缺少參數驗證、錯誤碼不一致、版本兼容性） | 重要 |
| REV-008 | Regression 風險審查（影響現有功能的可能性評估） | 必要 |
| REV-009 | 智慧規則審查（套用 Wisdom Engine 中的企業規則） | 必要 |
| REV-010 | 審查結果分級：PASS / WARN / FAIL | 必要 |
| REV-011 | FAIL 級別審查結果阻止 PR 建立 | 必要 |
| REV-012 | 審查結果儲存於 Review Memory | 必要 |

### 3.11 測試引擎（Testing Engine）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| TEST-001 | 自動生成 JUnit 5 單元測試 | 必要 |
| TEST-002 | 自動生成 Spring Boot 整合測試（`@SpringBootTest`） | 重要 |
| TEST-003 | 自動生成 API 測試（基於 OpenAPI 規格） | 重要 |
| TEST-004 | 自動執行測試並回報結果 | 必要 |
| TEST-005 | 測試覆蓋率報告（目標：新增程式碼 80%+） | 重要 |
| TEST-006 | Regression Test 套件管理（每次發布後更新） | 重要 |
| TEST-007 | 測試失敗時自動觸發 AI 修正迴圈 | 重要 |
| TEST-008 | 測試計劃作為規格的一部分，在 Spec 階段定義 | 必要 |

### 3.12 AI 介面卡（AI Adapter）

| 需求 ID | 需求描述 | 優先級 |
|---|---|---|
| AI-001 | 實作 Adapter Pattern，所有 AI 呼叫透過統一介面 | 必要 |
| AI-002 | 支援 GitHub Copilot（透過 API 或 CLI 橋接） | 必要 |
| AI-003 | 支援 Claude Code（Anthropic API） | 必要 |
| AI-004 | 支援 Gemini CLI（Google AI API） | 必要 |
| AI-005 | 支援 OpenAI API（GPT-4o 等） | 必要 |
| AI-006 | 支援 Ollama（本地 LLM） | 必要 |
| AI-007 | 支援 MCP（Model Context Protocol） | 重要 |
| AI-008 | AI 供應商可在設定中切換，不需修改程式碼 | 必要 |
| AI-009 | 每次 AI 呼叫記錄：供應商、Token 用量、延遲、成功/失敗 | 重要 |
| AI-010 | 支援 AI 供應商 Fallback（主要供應商失敗時自動切換備援） | 重要 |
| AI-011 | 傳送至 AI 的 Context 自動包含：任務規格 + 相關知識 + 記憶片段 + 程式碼上下文 | 必要 |

---

## 4. Human Checkpoint 需求

### 4.1 規格核准（Spec Approval）

| 項目 | 需求 |
|---|---|
| **觸發時機** | Specification Engine 完成規格生成後，在進入 Confidence Engine 前 |
| **顯示內容** | 需求摘要、知識引用清單、影響分析、風險等級、測試計劃 |
| **審核選項** | `approve`（核准）/ `reject`（拒絕，需填理由）/ `edit`（開啟編輯模式） |
| **CLI 格式** | 互動式顯示，支援翻頁瀏覽長規格 |
| **Web UI** | 結構化顯示，支援行內評論 |
| **IDE Plugin** | 彈出通知，點擊跳轉至 Web UI |
| **逾時處理** | 無逾時，人工核准前系統等待 |
| **稽核日誌** | 記錄：核准者、時間、版本、決策 |

### 4.2 影響核准（Impact Approval）

| 項目 | 需求 |
|---|---|
| **觸發條件** | 影響分析識別出 RiskLevel = HIGH，或影響超過 5 個模組 |
| **顯示內容** | 受影響模組清單、影響類型（修改 / 新增 / 刪除）、回滾計劃 |
| **審核選項** | `approve` / `reject` / `reduce-scope`（縮小變更範圍） |
| **核准人員** | 需技術負責人或架構師核准（可設定） |

### 4.3 任務核准（Task Approval）

| 項目 | 需求 |
|---|---|
| **觸發時機** | Planning Engine 完成任務分解後，在開始 AI Coding 前 |
| **顯示內容** | 任務清單（含順序、相依關係、信心分數）、總任務數、預估 Token 用量 |
| **審核選項** | `approve` / `reject` / `replan`（重新規劃任務） |

### 4.4 PR 核准（PR Approval）

| 項目 | 需求 |
|---|---|
| **觸發時機** | Review Engine 完成審查且所有任務執行完畢後 |
| **顯示內容** | Code Diff 摘要、審查結果摘要（各維度通過 / 警告）、測試結果、覆蓋率 |
| **審核選項** | `approve`（建立 PR）/ `reject`（列出修改意見，回到修正迴圈） |
| **Web UI** | 完整 Diff 檢視，支援行內評論，類似 GitHub PR Review |

---

## 5. 非功能性需求（NFR）

### 5.1 效能需求

| 指標 | 需求 |
|---|---|
| `aipa init`（首次初始化，10 萬行中型 Java 專案） | < 5 分鐘完成 |
| `aipa ask` 知識查詢回應時間 | < 3 秒 |
| Spec 生成時間 | < 30 秒 |
| 規格核准後到第一個任務開始 | < 10 秒 |
| 單任務 AI 呼叫（不含 AI 供應商延遲） | < 5 秒 |
| PR Merge 後 Learning 觸發延遲 | < 60 秒 |
| Runtime Service 記憶體使用（閒置狀態） | < 512 MB |
| Runtime Service 記憶體使用（活躍 Session） | < 2 GB |

### 5.2 安全性需求

| 需求 ID | 需求描述 |
|---|---|
| SEC-001 | 所有企業知識、記憶、程式碼上下文僅儲存於本地，絕不傳送至第三方雲端服務 |
| SEC-002 | AI 呼叫僅傳送必要的任務上下文，不傳送完整程式碼庫 |
| SEC-003 | AI Provider API Key 加密儲存（AES-256） |
| SEC-004 | Runtime API 不對外網開放（僅 localhost 或企業 LAN） |
| SEC-005 | 所有 Human Checkpoint 操作記錄稽核日誌 |
| SEC-006 | 支援設定哪些目錄/檔案不得被掃描或傳送給 AI |
| SEC-007 | 預設不記錄任何遙測數據（No Telemetry by default） |

### 5.3 可靠性需求

| 需求 ID | 需求描述 |
|---|---|
| REL-001 | Runtime Service 異常崩潰後可自動重啟 |
| REL-002 | 進行中的 Session 狀態持久化（崩潰後可恢復） |
| REL-003 | AI 呼叫失敗時自動重試（最多 3 次） |
| REL-004 | 知識庫資料定期自動備份 |
| REL-005 | 提供 `aipa health` 命令查詢系統狀態 |

### 5.4 可擴展性需求

| 需求 ID | 需求描述 |
|---|---|
| EXT-001 | AI Adapter 支援新供應商擴充，不需修改核心程式碼 |
| EXT-002 | Storage Backend 支援切換，不需遷移業務邏輯 |
| EXT-003 | Scanner 支援新技術棧 Plugin 擴充 |
| EXT-004 | Review Engine 審查規則可自訂擴充 |
| EXT-005 | Spec 模板可自訂 |

### 5.5 可維護性需求

| 需求 ID | 需求描述 |
|---|---|
| MAIN-001 | 所有模組提供健康檢查 Endpoint |
| MAIN-002 | 所有模組提供結構化日誌（JSON 格式） |
| MAIN-003 | 提供 `aipa doctor` 命令診斷常見問題 |
| MAIN-004 | 設定檔變更不需重啟服務（熱重載） |
| MAIN-005 | 提供遷移工具用於版本升級 |

---

## 6. CLI 命令需求

### 6.1 核心命令

| 命令 | 描述 |
|---|---|
| `aipa init` | 初始化專案，建立知識庫與 Project DNA |
| `aipa ask "<需求>"` | 輸入需求，啟動完整 LSDD 開發週期 |
| `aipa scan` | 重新掃描專案（全量或增量） |
| `aipa learn [--pr=<id>]` | 手動觸發學習（或指定 PR） |
| `aipa review` | 對目前工作目錄執行審查 |
| `aipa status` | 顯示目前 Session 狀態與進行中的任務 |
| `aipa checkpoint` | 顯示待審核的 Checkpoint 清單 |

### 6.2 知識管理命令

| 命令 | 描述 |
|---|---|
| `aipa knowledge list` | 列出知識庫條目 |
| `aipa knowledge add` | 手動新增知識項目 |
| `aipa knowledge search "<關鍵字>"` | 語意搜尋知識庫 |
| `aipa knowledge export` | 匯出知識庫為 Markdown |
| `aipa adr add` | 新增架構決策記錄（ADR） |

### 6.3 記憶管理命令

| 命令 | 描述 |
|---|---|
| `aipa memory list` | 列出記憶條目（按類型篩選） |
| `aipa memory show <id>` | 顯示特定記憶詳情 |

### 6.4 智慧規則命令

| 命令 | 描述 |
|---|---|
| `aipa wisdom list` | 列出所有智慧規則 |
| `aipa wisdom add` | 新增智慧規則 |
| `aipa wisdom edit <id>` | 編輯智慧規則 |

### 6.5 系統管理命令

| 命令 | 描述 |
|---|---|
| `aipa server start` | 啟動 Runtime Service |
| `aipa server stop` | 停止 Runtime Service |
| `aipa server status` | 查詢 Runtime Service 狀態 |
| `aipa health` | 全系統健康檢查 |
| `aipa doctor` | 診斷並修復常見問題 |
| `aipa config set <key> <value>` | 修改設定 |
| `aipa version` | 顯示版本資訊 |

---

## 7. Plugin 需求

### 7.1 功能同等性原則
所有 Plugin（CLI、VSCode Extension、IntelliJ Plugin、Web UI）共用相同的 AIPA Runtime Service。所有功能透過 REST API 呼叫，Plugin 僅為展示層。

### 7.2 VSCode Extension 需求

| 需求 ID | 需求描述 |
|---|---|
| VSC-001 | 側欄面板顯示目前 Session 狀態與任務進度 |
| VSC-002 | Human Checkpoint 通知（彈出提示，可直接核准/拒絕） |
| VSC-003 | 右鍵選單：「Ask AIPA」，以選取的程式碼為上下文發起 `aipa ask` |
| VSC-004 | 知識庫快速搜尋（Command Palette 整合） |
| VSC-005 | 顯示目前檔案的相關知識（Hover 提示） |

### 7.3 IntelliJ Plugin 需求

| 需求 ID | 需求描述 |
|---|---|
| IJ-001 | 工具視窗顯示目前 Session 狀態與任務進度 |
| IJ-002 | Human Checkpoint 通知（氣泡通知） |
| IJ-003 | 右鍵選單：「Ask AIPA」 |
| IJ-004 | 知識庫搜尋（Search Everywhere 整合） |
| IJ-005 | Gutter Icon 顯示相關知識項目 |

### 7.4 Web UI Dashboard 需求

| 需求 ID | 需求描述 |
|---|---|
| WEB-001 | Session 管理：列出所有歷史與進行中的 Session |
| WEB-002 | Human Checkpoint 管理：集中檢視所有待審核項目 |
| WEB-003 | 知識庫瀏覽器：樹狀結構瀏覽知識項目 |
| WEB-004 | 記憶管理：按類型瀏覽記憶條目 |
| WEB-005 | 學習歷史：顯示每次 PR Merge 的學習結果 |
| WEB-006 | Spec 瀏覽器：瀏覽所有歷史規格文件 |
| WEB-007 | PR Diff 檢視：供 PR Approval 使用的完整 Diff 介面 |
| WEB-008 | 系統監控：Runtime Service 狀態、記憶體用量、API 呼叫統計 |
| WEB-009 | 多專案切換：在不同專案的 AIPA Context 間切換 |

---

## 8. Installer 需求

### 8.1 Windows MSI Installer

| 需求 ID | 需求描述 |
|---|---|
| INS-W-001 | 提供圖形化安裝精靈（GUI Wizard） |
| INS-W-002 | 捆綁 JRE 17（使用者無需預先安裝 Java） |
| INS-W-003 | 捆綁 Node.js 20 LTS（CLI 依賴） |
| INS-W-004 | 捆綁 Python 3.11（AI Engine 依賴） |
| INS-W-005 | 安裝完成後自動將 `aipa` 加入系統 PATH |
| INS-W-006 | 安裝完成後自動將 AIPA Runtime Service 註冊為 Windows Service |
| INS-W-007 | 提供解除安裝程式（清理所有已安裝元件） |
| INS-W-008 | 支援靜默安裝模式（`setup.exe /quiet`，企業批量部署） |

### 8.2 Linux Shell Installer

| 需求 ID | 需求描述 |
|---|---|
| INS-L-001 | 提供單行安裝命令（`curl -sSL https://... | bash`） |
| INS-L-002 | 自動檢測並安裝系統相依（Java / Node.js / Python） |
| INS-L-003 | 安裝完成後自動註冊 systemd service |
| INS-L-004 | 支援 Ubuntu 22.04 LTS、CentOS 8+、RHEL 8+ |
| INS-L-005 | 離線安裝模式（提供離線安裝包） |

### 8.3 Docker Compose Installer

| 需求 ID | 需求描述 |
|---|---|
| INS-D-001 | 提供官方 `docker-compose.yml` 與 `.env.example` |
| INS-D-002 | 一行命令啟動：`docker compose up -d` |
| INS-D-003 | 所有服務資料掛載至本地 Volume（資料不在容器內） |
| INS-D-004 | 支援 PostgreSQL 選用設定（預設 SQLite） |
| INS-D-005 | 提供 `docker compose pull` 更新命令 |
| INS-D-006 | 健康檢查設定確保服務正確啟動順序 |

---

## 9. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始產品需求文件 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*
