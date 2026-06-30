# Phase 9 企業強化（目前已落地項目）

## 已完成（MVP Slice）

### 1) CLI 診斷命令 `aipa doctor`

位置：`cli/src/index.ts`

- 檢查 Runtime 連線狀態
- 檢查 Node.js 版本（建議 `>=20`）
- 檢查 AI 供應商設定（API Key / Ollama）
- 檢查工作目錄可寫入權限
- 檢查 `AIPA_CONTEXT_EXCLUDE_PATTERNS` 是否設定
- 支援 `--json` 結構化輸出，便於 CI 解析

範例：

```bash
cd cli
npm run build
node ./dist/index.js doctor --json
```

### 2) Context Exclude 安全遮罩

位置：`cli/src/index.ts`

- 內建遮罩規則：`apiKey`、`token`、`password`、`secret`、常見金鑰前綴
- 支援自訂規則：環境變數 `AIPA_CONTEXT_EXCLUDE_PATTERNS`
- 已套用到：
  - `aipa ask <requirement>`
  - `aipa wisdom check --diff <codeDiff>`

效果：送往 Runtime 前先遮罩敏感資訊，避免憑證外洩風險。

### 3) 自動化測試

位置：`cli/tests/phase9.e2e.test.ts`

- 驗證 `doctor --json` 有結構化輸出
- 驗證 `ask` 會先遮罩敏感字串再送出
- 驗證 `wisdom check` 會先遮罩 diff 內容再送出

## 後續建議（下一個 Slice）

1. Runtime API IP 白名單（Spring Filter）
2. API Key OS Keychain 儲存（Windows Credential Manager / Secret Service）
3. 結構化 JSON 日誌全鏈路化（Runtime + AI Engine + CLI）
4. `aipa doctor --fix` 自動修復常見環境問題


