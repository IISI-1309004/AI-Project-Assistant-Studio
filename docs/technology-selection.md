# AIPA Studio — 技術選型（Technology Selection）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 架構團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：[系統架構文件](./sad.md)、[模組設計](./module-design.md)

---

## 1. 技術選型原則

所有技術選擇必須同時滿足以下四個標準：

| 標準 | 說明 |
|---|---|
| **企業適用性** | 技術必須具備生產環境穩定性，有長期維護承諾 |
| **授權相容性** | 所有相依必須為 Apache 2.0 或 MIT 授權，可商業使用 |
| **本地部署支援** | 核心功能不依賴任何雲端服務，可完全離線運作 |
| **跨平台** | 支援 Windows 10+、Ubuntu 22.04 LTS、RHEL 8+、macOS（開發環境） |

---

## 2. Runtime Core 技術棧（`runtime/`、`scanner/`、`agent/`、`workflow/`）

### 2.1 Java 執行期

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **Java** | 17 LTS | 長期支援版本（2029 年 EOL）；目標使用者熟悉 Java；`record`、`sealed class` 等新語法提升可讀性 | GPL v2 with Classpath Exception |
| **Spring Boot** | 3.3.x | 企業 Java 標準框架；原生支援 REST API、JPA、Security、Batch；活躍社群 | Apache 2.0 |
| **Spring Data JPA** | （隨 Spring Boot 3.3） | 統一 Repository 抽象，支援 SQLite / PostgreSQL 切換 | Apache 2.0 |
| **Spring Security** | （隨 Spring Boot 3.3） | Runtime API 存取控制（IP 白名單、基本認證） | Apache 2.0 |
| **Flyway** | 10.x | 資料庫 Schema 版本化遷移；支援 SQLite 與 PostgreSQL | Apache 2.0 |
| **HikariCP** | （隨 Spring Boot） | 高效能連線池；PostgreSQL 部署時使用 | Apache 2.0 |

### 2.2 建構工具

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **Gradle** | 8.x | Kotlin DSL 設定可讀性高；Multi-project 支援完善；比 Maven 建構速度快 | Apache 2.0 |
| **Gradle Wrapper** | （隨 Gradle 8.x） | 確保所有開發人員使用相同 Gradle 版本 | Apache 2.0 |

### 2.3 Java 靜態分析（Scanner Engine 使用）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **JavaParser** | 3.25.x | Java 原始碼 AST 解析；分析類別結構、方法呼叫 | Apache 2.0 / LGPL v3 |
| **ASM** | 9.x | Java 位元組碼分析（補充 AST 分析） | BSD 3-Clause |
| **JAXB** | （JDK 內建） | XML 解析（MyBatis Mapper、Spring XML 設定） | CDDL / GPL v2 |
| **SnakeYAML** | 2.x | YAML 解析（application.yml、docker-compose.yml） | Apache 2.0 |
| **Swagger Parser** | 2.x | OpenAPI 3.0 / Swagger 2.0 規格解析 | Apache 2.0 |

### 2.4 HTTP 客戶端（AI Adapter 使用）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **OkHttp** | 4.x | 呼叫 Claude / Gemini / OpenAI REST API；支援逾時與重試 | Apache 2.0 |
| **Jackson** | 2.x（隨 Spring Boot） | JSON 序列化 / 反序列化 | Apache 2.0 |

### 2.5 測試框架（Java）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **JUnit 5** | 5.10.x | Java 單元測試標準框架 | EPL 2.0 |
| **Mockito** | 5.x | Mock 物件框架 | MIT |
| **Spring Boot Test** | （隨 Spring Boot） | Spring 整合測試 | Apache 2.0 |
| **Testcontainers** | 1.19.x | 整合測試使用真實 PostgreSQL 容器 | MIT |
| **RestAssured** | 5.x | API 測試框架 | Apache 2.0 |

---

## 3. AI Engine 技術棧（`knowledge/`、`memory/`、`learning/`、`experience/`、`wisdom/`）

### 3.1 Python 執行期

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **Python** | 3.11 LTS | AI/ML 生態系第一語言；3.11 效能顯著優於 3.10；企業常用版本 | PSF License |
| **FastAPI** | 0.111.x | 高效能非同步 REST API 框架；自動生成 OpenAPI 文件；Pydantic v2 整合 | MIT |
| **Uvicorn** | 0.29.x | ASGI 伺服器，FastAPI 標準搭配；高效能非同步 I/O | BSD |
| **Pydantic** | 2.x | 資料驗證與 Schema 定義；Python typing 完整支援 | MIT |

### 3.2 AI / 向量處理

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **LangChain** | 0.2.x | LLM 工作流程框架；Prompt 管理；輸出解析 | MIT |
| **LlamaIndex** | 0.10.x | 知識索引與檢索框架；支援多種資料來源 | MIT |
| **sentence-transformers** | 3.x | 文字向量嵌入（Embedding）；支援本地模型 | Apache 2.0 |
| **ChromaDB** | 0.5.x | 本地向量資料庫；零外部依賴；支援持久化 | Apache 2.0 |
| **numpy** | 1.26.x | 向量運算基礎庫 | BSD |

### 3.3 預設 Embedding 模型

| 模型 | 來源 | 用途 | 授權 |
|---|---|---|---|
| `all-MiniLM-L6-v2` | Sentence Transformers Hub | 知識項目向量化（輕量，適合本地） | Apache 2.0 |
| `paraphrase-multilingual-MiniLM-L12-v2` | Sentence Transformers Hub | 多語言支援（中文 / 日文 / 英文） | Apache 2.0 |

> **說明**：預設使用本地 Embedding 模型（sentence-transformers），不需要任何外部 API。模型在首次安裝時自動下載並快取至本地。

### 3.4 資料存取（Python）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **SQLAlchemy** | 2.x | ORM 與 SQL 工具組；支援 SQLite / PostgreSQL | MIT |
| **Alembic** | 1.13.x | Python 資料庫 Schema 遷移工具 | MIT |
| **psycopg2-binary** | 2.9.x | PostgreSQL Python 驅動（二進位版本，免安裝 libpq） | LGPL |

### 3.5 Git 分析（Learning Engine 使用）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **GitPython** | 3.1.x | Git Repository 操作；讀取 diff、commit、log | BSD |
| **pygit2** | 1.14.x | 高效能 Git 操作（底層 libgit2） | GPL v2 with Linking Exception |

### 3.6 建構工具（Python）

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **Poetry** | 1.8.x | 相依管理與虛擬環境；lock 檔確保可重現建構；Monorepo 工作空間支援 | MIT |

### 3.7 測試框架（Python）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **pytest** | 8.x | Python 標準測試框架 | MIT |
| **pytest-asyncio** | 0.23.x | 非同步 FastAPI 端點測試 | Apache 2.0 |
| **httpx** | 0.27.x | FastAPI 測試用 HTTP 客戶端 | BSD |

---

## 4. CLI 技術棧（`cli/`）

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **Node.js** | 20 LTS | 長期支援（2026 年 EOL）；跨平台 CLI 生態豐富；npm 套件庫完整 | MIT |
| **TypeScript** | 5.x | 型別安全；重構友善；IDE 支援完整 | Apache 2.0 |
| **Commander.js** | 12.x | CLI 命令解析框架；子命令樹結構；自動生成 `--help` | MIT |
| **Inquirer.js** | 10.x | 互動式 Terminal 提示（Human Checkpoint 審核介面） | MIT |
| **Chalk** | 5.x | Terminal 彩色輸出 | MIT |
| **ora** | 8.x | Terminal Loading Spinner（Session 進度） | MIT |
| **eventsource** | 2.x | SSE 客戶端（訂閱 Runtime Session 進度串流） | MIT |
| **axios** | 1.x | HTTP 客戶端（呼叫 Runtime REST API） | MIT |
| **js-yaml** | 4.x | 讀寫 `.ai-project/config.yml` | MIT |
| **tsx** | 4.x | 直接執行 TypeScript（開發期） | MIT |
| **pkg** | 5.x | 打包 Node.js 應用為獨立可執行檔 | MIT |

### 建構工具（CLI）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **tsup** | 8.x | TypeScript 打包（基於 esbuild，極快） | MIT |
| **vitest** | 1.x | 單元測試框架 | MIT |

---

## 5. Web UI 技術棧（`web/`）

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **React** | 18.x | 企業前端標準；生態完整；Hook 模型成熟 | MIT |
| **TypeScript** | 5.x | 型別安全；與後端 API 契約可共享型別定義 | Apache 2.0 |
| **Vite** | 5.x | 開發期極快 HMR；生產建構基於 Rollup | MIT |
| **React Router** | 6.x | SPA 路由管理 | MIT |
| **TailwindCSS** | 3.x | Utility-first CSS；無需設計系統即可快速開發 | MIT |
| **React Query（TanStack Query）** | 5.x | 伺服器狀態管理；API 呼叫快取與同步 | MIT |
| **Zustand** | 4.x | 輕量 Client 狀態管理（全域 UI 狀態） | MIT |
| **Monaco Editor** | 0.47.x | VSCode 核心編輯器元件（用於 Spec 編輯與 Diff 檢視） | MIT |
| **react-diff-view** | 3.x | PR Code Diff 顯示元件（類 GitHub 風格） | MIT |
| **react-flow** | 11.x | 知識圖譜視覺化（節點 / 邊關係圖） | MIT |

### 建構工具（Web）

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **vitest** | 1.x | 單元測試 | MIT |
| **Playwright** | 1.x | E2E 測試 | Apache 2.0 |

---

## 6. IDE Plugin 技術棧

### 6.1 VSCode Extension（`plugin/vscode/`）

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **TypeScript** | 5.x | VSCode Extension 官方語言 | Apache 2.0 |
| **VSCode Extension API** | 1.89+ | 擴充功能入口；Webview、TreeView、StatusBar、Command | MIT |
| **@vscode/vsce** | 2.x | 打包 `.vsix` 安裝包 | MIT |

### 6.2 IntelliJ Plugin（`plugin/intellij/`）

| 技術 | 版本 | 選用理由 | 授權 |
|---|---|---|---|
| **Kotlin** | 1.9.x | IntelliJ Platform 官方推薦語言；與 Java 完全互通 | Apache 2.0 |
| **IntelliJ Platform SDK** | 2024.1+ | Plugin 開發框架；ToolWindow、Action、Notification | Apache 2.0 |
| **JCEF（Java Chromium Embedded Framework）** | （隨 IntelliJ） | 在 ToolWindow 中嵌入 Web UI（載入 React SPA） | BSD |
| **Gradle IntelliJ Plugin** | 1.17.x | IntelliJ Plugin 建構工具 | Apache 2.0 |

---

## 7. 儲存技術棧

### 7.1 關聯式資料庫

| 技術 | 版本 | 使用場景 | 授權 |
|---|---|---|---|
| **SQLite** | 3.45.x | 預設儲存後端（單機開發人員）；零額外依賴；跨平台 | Public Domain |
| **PostgreSQL** | 15.x | 企業團隊共享儲存後端；支援並發讀寫；完整 ACID | PostgreSQL License |
| **H2** | 2.x | Java 單元測試 / 整合測試使用（記憶體模式） | EPL 1.0 / MPL 2.0 |

### 7.2 向量資料庫

| 技術 | 版本 | 使用場景 | 授權 |
|---|---|---|---|
| **ChromaDB** | 0.5.x | 本地向量儲存（預設）；零外部依賴；支援持久化 | Apache 2.0 |
| **pgvector**（可選） | 0.7.x | PostgreSQL Extension，在同一個 DB 儲存向量（企業升級選項） | PostgreSQL License |

### 7.3 全文搜尋（可選升級）

| 技術 | 版本 | 使用場景 | 授權 |
|---|---|---|---|
| **Elasticsearch** | 8.x | 知識庫全文搜尋增強（大型企業選項） | SSPL / Elastic License 2.0 |

> **⚠️ 注意**：Elasticsearch 8.x 授權為 SSPL，用於內部工具使用時無需商業授權，但需確認企業法務審核。

---

## 8. AI 供應商 SDK

| 供應商 | SDK / 整合方式 | 版本 | 授權 |
|---|---|---|---|
| **Anthropic Claude** | `anthropic` Python SDK | 0.28.x | MIT |
| **Google Gemini** | `google-generativeai` Python SDK | 0.7.x | Apache 2.0 |
| **OpenAI** | `openai` Python SDK | 1.30.x | MIT |
| **Ollama** | 直接呼叫 Ollama REST API（`localhost:11434`） | N/A | MIT |
| **GitHub Copilot** | `gh copilot` CLI 橋接（Java 子進程呼叫） | 最新版 | GitHub 服務條款 |
| **MCP** | `mcp` Python SDK | 1.x | MIT |

> **設計說明**：所有 AI SDK 呼叫集中在 `aipa-agent`（Java）或 AI Engine（Python）內，業務邏輯不直接依賴任何 AI SDK。供應商切換只需更換 Adapter 實作。

---

## 9. Installer 工具鏈

### 9.1 Windows MSI

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **NSIS（Nullsoft Scriptable Install System）** | 3.10 | Windows 安裝精靈生成；支援靜默安裝；免費 | zlib/libpng License |
| **WiX Toolset**（備選） | 4.x | MSI 格式安裝包；企業 GPO 部署相容 | MIT |
| **Launch4j** | 3.50 | 將 JAR 包裝為 Windows `.exe` | BSD / MIT |

### 9.2 Linux Shell

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **Bash** | 5.x | 安裝 / 更新 / 解除安裝腳本 | GPL v3 |
| **systemd** | N/A | Linux 服務管理 | LGPL |
| **Nginx** | 1.26.x | Web UI 靜態服務與 API 反向代理 | BSD |

### 9.3 Docker Compose

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **Docker Compose** | v2（Plugin 形式） | 容器編排；服務依賴管理 | Apache 2.0 |
| **Docker** | 26.x | 容器執行期 | Apache 2.0 |

---

## 10. CI/CD 工具鏈

| 技術 | 版本 | 用途 | 授權 |
|---|---|---|---|
| **GitHub Actions** | N/A | CI/CD 流水線；自動化建構 / 測試 / 發布 | GitHub 服務條款 |
| **SonarCloud**（可選） | N/A | 程式碼品質掃描 | SonarSource 授權 |
| **Trivy** | 0.51.x | 容器映像安全掃描 | Apache 2.0 |

### CI 流水線觸發條件

```yaml
# .github/workflows/ci.yml 觸發條件
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
```

### CI 流水線各語言任務

| 語言 | 任務 |
|---|---|
| Java | `./gradlew build test` |
| Python | `poetry run pytest` + `poetry run ruff check` |
| TypeScript | `npm run build --workspaces` + `npm run test --workspaces` |

---

## 11. 開發工具建議

| 工具 | 版本 | 用途 |
|---|---|---|
| **IntelliJ IDEA** | 2024.1+ | Java / Kotlin 開發（Runtime / Scanner / Plugin） |
| **VSCode** | 最新版 | TypeScript / Python 開發（CLI / Web / AI Engine） |
| **Docker Desktop** | 4.x | 本地開發環境（Docker Compose） |
| **DBeaver** | 23.x | SQLite / PostgreSQL 資料庫管理 |
| **Postman / Bruno** | 最新版 | Runtime REST API 測試 |

---

## 12. 技術版本鎖定策略

### 12.1 版本鎖定文件

| 語言 | 鎖定文件 | 說明 |
|---|---|---|
| Java | `build.gradle.kts`（`implementation("...")`） | 明確指定版本，不使用 `latest` |
| Python | `poetry.lock` | Poetry 自動生成，確保完全可重現 |
| Node.js | `package-lock.json` | npm 自動生成 |

### 12.2 版本升級政策

- **安全性修補（Patch）**：每月自動升級，CI 驗證後合并
- **次要版本（Minor）**：每季評估，需通過完整測試套件
- **主要版本（Major）**：需架構審查，列入路線圖規劃
- **Java LTS 版本**：Java 17 → Java 21 於 Phase 3 之後評估

---

## 13. 技術排除清單（明確不採用）

以下技術因特定原因明確排除，未來不得在未經架構審查的情況下引入：

| 排除技術 | 排除原因 |
|---|---|
| Lombok | 增加 IDE 設定複雜度；Java 14+ record 類別可取代 |
| Kotlin（Java 模組） | 維持 Java 主線；僅 IntelliJ Plugin 使用 Kotlin |
| MongoDB | 非結構化儲存不適合知識庫的關聯查詢需求 |
| Redis | 不引入額外有狀態服務；Session 狀態已由 StorageProvider 管理 |
| Kafka / RabbitMQ | 單一進程事件匯流排已足夠；不引入分散式訊息佇列 |
| Django / Flask | FastAPI 效能與現代化程度更優；Pydantic v2 整合更佳 |
| Angular / Vue | 統一使用 React，降低前端技術多樣性 |
| GraphQL | REST API 已足夠；GraphQL 增加前後端複雜度 |
| gRPC | 跨語言通訊已透過 REST API 實現；不引入 Protobuf 依賴 |

---

## 14. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始技術選型文件 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。架構鎖定後，不得在未經架構審查的情況下新增或更換技術。*
