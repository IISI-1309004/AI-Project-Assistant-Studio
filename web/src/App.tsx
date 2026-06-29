import React from "react";
import { SessionsView } from "./components/SessionsView";
import { CheckpointPanel } from "./components/CheckpointPanel";
import { SystemStatus } from "./components/SystemStatus";

/**
 * AIPA Studio Web Dashboard — Phase 7 完整實作
 * 功能：會話管理、檢查點核審、系統監控
 */
function App() {
  return (
    <div style={{ minHeight: "100vh", background: "#ffffff", fontFamily: "system-ui, sans-serif" }}>
      {/* 健檔區 */}
      <header
        style={{
          background: "#f7f8fa",
          borderBottom: "1px solid #e5e7eb",
          padding: "1rem 2rem",
          boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
        }}
      >
        <div style={{ maxWidth: "1400px", margin: "0 auto" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "2rem" }}>
            <div>
              <h1 style={{ margin: 0, fontSize: "1.5rem", fontWeight: 700, color: "#1f2328" }}>
                🤖 AIPA Studio Dashboard
              </h1>
              <p style={{ margin: "0.5rem 0 0", fontSize: "0.875rem", color: "#57606a" }}>
                AI 專案協助工作室 — 工作階段與檢查點管理
              </p>
            </div>
            <div style={{ width: "300px" }}>
              <SystemStatus />
            </div>
          </div>
        </div>
      </header>

      {/* 主內容區 */}
      <main
        style={{
          maxWidth: "1400px",
          margin: "0 auto",
          padding: "2rem",
          display: "flex",
          gap: "2rem",
          minHeight: "calc(100vh - 150px)",
        }}
      >
        {/* 左側欄 — 工作階段列表 */}
        <aside
          style={{
            width: "320px",
            background: "#ffffff",
            border: "1px solid #e5e7eb",
            borderRadius: "6px",
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <SessionsView />
        </aside>

        {/* 右側欄 — 檢查點核審 */}
        <div
          style={{
            flex: 1,
            background: "#ffffff",
            border: "1px solid #e5e7eb",
            borderRadius: "6px",
            overflow: "hidden",
            display: "flex",
          }}
        >
          <CheckpointPanel />
        </div>
      </main>

      {/* 頁腳 */}
      <footer
        style={{
          background: "#f7f8fa",
          borderTop: "1px solid #e5e7eb",
          padding: "1rem 2rem",
          textAlign: "center",
          fontSize: "0.75rem",
          color: "#57606a",
        }}
      >
        <p>
          AIPA Studio Phase 7 © 2026 —
          <a href="http://localhost:8080/api/v1/health" style={{ color: "#0969da", marginLeft: "0.5rem" }}>
            Runtime Health Check
          </a>
        </p>
      </footer>
    </div>
  );
}

export default App;
