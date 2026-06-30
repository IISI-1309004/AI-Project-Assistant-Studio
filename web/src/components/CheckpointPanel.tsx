import React, { useEffect, useState } from "react";
import { useAipaStore } from "../store";

export const CheckpointPanel: React.FC = () => {
  const {
    checkpoints,
    selectedCheckpointId,
    loadingCheckpoints,
    fetchCheckpoints,
    approveCheckpoint,
    rejectCheckpoint,
  } = useAipaStore();

  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState("");

  useEffect(() => {
    void fetchCheckpoints();
    const interval = setInterval(() => void fetchCheckpoints(), 5000);
    return () => clearInterval(interval);
  }, [fetchCheckpoints]);

  const selectedCheckpoint = checkpoints.find((c) => c.checkpointId === selectedCheckpointId);

  const handleApprove = async () => {
    if (!selectedCheckpointId) return;
    setActionLoading(true);
    setActionMessage("");
    try {
      await approveCheckpoint(selectedCheckpointId);
      setActionMessage("✅ 已核准");
      setTimeout(() => setActionMessage(""), 3000);
    } catch (error) {
      setActionMessage("❌ 核准失敗：" + String(error));
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selectedCheckpointId) return;
    setActionLoading(true);
    setActionMessage("");
    try {
      await rejectCheckpoint(selectedCheckpointId);
      setActionMessage("✅ 已拒絕");
      setTimeout(() => setActionMessage(""), 3000);
    } catch (error) {
      setActionMessage("❌ 拒絕失敗：" + String(error));
    } finally {
      setActionLoading(false);
    }
  };

  const pendingCheckpoints = checkpoints.filter(
    (c) => (c.status ?? "待審核").toUpperCase() === "PENDING" || (c.status ?? "待審核") === "待審核"
  );

  return (
    <div
      style={{
        flex: 1,
        display: "flex",
        flexDirection: "column",
        padding: "1rem",
        borderLeft: "1px solid #e5e7eb",
      }}
    >
      <div style={{ marginBottom: "1rem" }}>
        <h2 style={{ fontSize: "1.125rem", fontWeight: 600, margin: 0 }}>
          ✅ 檢查點核審
        </h2>
        <p style={{ fontSize: "0.875rem", color: "#57606a", margin: "0.5rem 0 0" }}>
          待審核：{pendingCheckpoints.length} 個
        </p>
      </div>

      {pendingCheckpoints.length === 0 ? (
        <div
          style={{
            background: "#f0fff4",
            border: "1px solid #34d399",
            borderRadius: "4px",
            padding: "1rem",
            textAlign: "center",
            color: "#057a55",
            fontSize: "0.875rem",
          }}
        >
          ✨ 所有檢查點已審核
        </div>
      ) : !selectedCheckpoint ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
          {pendingCheckpoints.map((cp) => (
            <div
              key={cp.checkpointId}
              onClick={() => {
                const { setSelectedCheckpoint } = useAipaStore.getState();
                setSelectedCheckpoint(cp.checkpointId);
              }}
              style={{
                padding: "0.75rem",
                background: "#fff7ed",
                border: "1px solid #fb923c",
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                {cp.type ?? "檢查點"} - {cp.checkpointId}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#92400e", marginTop: "0.25rem" }}>
                {cp.status ?? "待審核"}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: "1rem" }}>
          {/* 檢查點資訊 */}
          <div style={{ background: "#f7f8fa", border: "1px solid #e5e7eb", borderRadius: "4px", padding: "1rem" }}>
            <div style={{ fontSize: "0.875rem", color: "#57606a" }}>
              <strong>檢查點類型：</strong> {selectedCheckpoint.type ?? "檢查點"}
            </div>
            <div style={{ fontSize: "0.875rem", color: "#57606a", marginTop: "0.5rem" }}>
              <strong>ID：</strong> {selectedCheckpoint.checkpointId}
            </div>
            {selectedCheckpoint.sessionId && (
              <div style={{ fontSize: "0.875rem", color: "#57606a", marginTop: "0.5rem" }}>
                <strong>工作階段：</strong> {selectedCheckpoint.sessionId}
              </div>
            )}
          </div>

          {/* Spec 檢視 */}
          {selectedCheckpoint.spec && (
            <div
              style={{
                background: "#f7f8fa",
                border: "1px solid #e5e7eb",
                borderRadius: "4px",
                padding: "1rem",
              }}
            >
              <strong style={{ display: "block", marginBottom: "0.5rem" }}>📋 規格</strong>
              <pre
                style={{
                  fontSize: "0.75rem",
                  color: "#57606a",
                  overflow: "auto",
                  maxHeight: "200px",
                  margin: 0,
                  whiteSpace: "pre-wrap",
                }}
              >
                {selectedCheckpoint.spec}
              </pre>
            </div>
          )}

          {/* Diff 檢視 */}
          {selectedCheckpoint.diff && (
            <div
              style={{
                background: "#f0f9ff",
                border: "1px solid #06b6d4",
                borderRadius: "4px",
                padding: "1rem",
              }}
            >
              <strong style={{ display: "block", marginBottom: "0.5rem" }}>📝 程式碼變更</strong>
              <pre
                style={{
                  fontSize: "0.75rem",
                  color: "#164e63",
                  overflow: "auto",
                  maxHeight: "200px",
                  margin: 0,
                  whiteSpace: "pre-wrap",
                  fontFamily: "monospace",
                }}
              >
                {selectedCheckpoint.diff}
              </pre>
            </div>
          )}

          {/* 操作按鈕 */}
          <div style={{ display: "flex", gap: "0.75rem" }}>
            <button
              onClick={handleApprove}
              disabled={actionLoading}
              style={{
                flex: 1,
                padding: "0.75rem",
                background: "#28a745",
                color: "white",
                border: "none",
                borderRadius: "4px",
                cursor: actionLoading ? "not-allowed" : "pointer",
                fontWeight: 500,
                opacity: actionLoading ? 0.6 : 1,
              }}
            >
              {actionLoading ? "處理中..." : "✅ 核准"}
            </button>
            <button
              onClick={handleReject}
              disabled={actionLoading}
              style={{
                flex: 1,
                padding: "0.75rem",
                background: "#dc3545",
                color: "white",
                border: "none",
                borderRadius: "4px",
                cursor: actionLoading ? "not-allowed" : "pointer",
                fontWeight: 500,
                opacity: actionLoading ? 0.6 : 1,
              }}
            >
              {actionLoading ? "處理中..." : "❌ 拒絕"}
            </button>
          </div>

          {actionMessage && (
            <div
              style={{
                padding: "0.75rem",
                background: actionMessage.startsWith("✅") ? "#d4edda" : "#f8d7da",
                color: actionMessage.startsWith("✅") ? "#155724" : "#721c24",
                borderRadius: "4px",
                fontSize: "0.875rem",
                textAlign: "center",
              }}
            >
              {actionMessage}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

