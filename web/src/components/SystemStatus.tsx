import React, { useEffect } from "react";
import { useAipaStore } from "../store";

export const SystemStatus: React.FC = () => {
  const { runtimeStatus, loadingHealth, fetchHealth } = useAipaStore();

  useEffect(() => {
    void fetchHealth();
    const interval = setInterval(() => void fetchHealth(), 15000);
    return () => clearInterval(interval);
  }, [fetchHealth]);

  const isOnline = runtimeStatus === "連線中";

  return (
    <div
      style={{
        padding: "0.75rem 1rem",
        background: isOnline ? "#dcfce7" : "#fee2e2",
        border: `1px solid ${isOnline ? "#86efac" : "#fca5a5"}`,
        borderRadius: "4px",
        display: "flex",
        alignItems: "center",
        gap: "0.5rem",
        fontSize: "0.875rem",
      }}
    >
      <span style={{ fontSize: "1rem" }}>
        {isOnline ? "🟢" : "🔴"}
      </span>
      <span style={{ color: isOnline ? "#166534" : "#7f1d1d", fontWeight: 500 }}>
        執行環境：{runtimeStatus}
      </span>
      {loadingHealth && (
        <span style={{ marginLeft: "auto", color: "#57606a" }}>
          同步中...
        </span>
      )}
    </div>
  );
};
