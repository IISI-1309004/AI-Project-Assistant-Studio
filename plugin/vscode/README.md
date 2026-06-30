# AIPA Studio VSCode 擴充功能

Phase 7 最小可交付版本。

## 功能

- 活動欄容器 `AIPA`
- 工作階段與檢查點的樹狀檢視
- 編輯器快選選單可執行 `AIPA：詢問`
- 待審核檢查點通知並支援行內 `核准` / `拒絕`
- 狀態列摘要（最新工作階段狀態 + 待審核檢查點數）

## 設定

在 VSCode 設定中：

- `aipa.runtimeUrl`（預設：`http://localhost:8080`）
- `aipa.pollIntervalSeconds`（預設：`10`，最小 `5`）

## 本機開發

```bash
cd plugin/vscode
npm install
npm run build
```

然後在 VSCode 中按下 `F5` 以啟動擴充功能開發主機。

## 建置 VSIX

```bash
cd plugin/vscode
npm run package:pre
npm run package
```

產生的 `.vsix` 檔案可透過以下方式安裝：

```bash
code --install-extension aipa-studio-vscode-1.0.0.vsix
```

