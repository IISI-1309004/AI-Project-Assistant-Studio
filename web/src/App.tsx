import React from "react";

/**
 * AIPA Studio Web UI — Phase 1 骨架
 * 所有頁面為空白佔位，Phase 7 實作完整 UI
 */
function App() {
  return (
    <div style={{ fontFamily: "system-ui, sans-serif", padding: "2rem", maxWidth: "760px", margin: "0 auto" }}>
      <h1 style={{ color: "#1f2328", borderBottom: "1px solid #e5e7eb", paddingBottom: "1rem" }}>
        AIPA Studio
      </h1>
      <p style={{ color: "#57606a" }}>
        AI Project Assistant Studio — Phase 1 骨架
      </p>
      <div style={{ background: "#f7f8fa", border: "1px solid #e5e7eb", borderRadius: "6px", padding: "1rem", marginTop: "1rem" }}>
        <strong>系統狀態</strong>
        <ul style={{ marginTop: "0.5rem", color: "#57606a" }}>
          <li>✅ Web UI 骨架已載入</li>
          <li>⏳ Phase 7：完整 Dashboard UI</li>
          <li>⏳ Runtime API: <a href="http://localhost:18080/api/v1/health">http://localhost:18080/api/v1/health</a></li>
        </ul>
      </div>
    </div>
  );
}

export default App;
