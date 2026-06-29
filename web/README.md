# AIPA Studio Web Dashboard

Phase 7 最小可交付版本。

## 功能

- 📊 **會話管理** — 檢視所有 AI 建置工作階段
- ✅ **檢查點核審** — 審查並核准/拒絕 AI 生成程式碼
- 🔴 **系統監控** — 即時監控執行環境連線狀態
- 📝 **規格檢視** — 查看 Spec 與程式碼變更 (Diff)
- 🎨 **現代化 UI** — 響應式設計、即時更新

## 技術棧

- React 18.3 + TypeScript
- Vite 5.2 (開發伺服器與打包)
- Zustand 4.5 (狀態管理)
- Axios 1.7 (HTTP 客戶端)
- Tailwind CSS 3.4 (未在此版本使用，備用)

## 開發

```bash
cd web
npm install
npm run dev
```

應用程式於 `http://localhost:5173` 啟動（預設）。

## 環境變數

- `.env.development` — 開發環境（API: `http://localhost:8080`）
- `.env.production` — 生產環境（API: `/api`）

## 編譯

```bash
npm run build
```

產生 `dist/` 目錄。

## API 連線

確保 AIPA Runtime 於 `http://localhost:8080` 運行。
可透過系統狀態指示器確認連線狀態。

