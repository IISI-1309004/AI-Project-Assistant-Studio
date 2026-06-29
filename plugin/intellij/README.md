# AIPA Studio IntelliJ 外掛程式

Phase 7 最小可交付版本。

## 功能

- 工具視窗（右側邊欄）
- Checkpoint 定期輪詢與通知
- 編輯器右鍵選單「詢問 AIPA」
- 專案工作階段建立

## 設定

在 IntelliJ IDE 中：

- 系統屬性 `aipa.runtime.url`（預設：`http://localhost:8080`）
- 系統屬性 `aipa.poll.interval`（預設：`10000` 毫秒）

## 開發

```bash
cd plugin/intellij
./gradlew build
```

執行 IDE 進行測試。

## 安裝

使用生成的 `.zip` 檔案：

1. IntelliJ IDEA → 設定 → 外掛程式
2. 齒輪圖示 → 以檔案安裝外掛程式
3. 選擇 `plugin/intellij/build/distributions/intellij-1.0.0-SNAPSHOT.zip`

重啟 IDE 後，AIPA 工具視窗應出現在右側邊欄。

