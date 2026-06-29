import React, { useEffect } from "react";
import { useAipaStore } from "../store";

export const SessionsView: React.FC = () => {
  const {
    sessions,
    selectedSessionId,
    loadingSessions,
    fetchSessions,
    setSelectedSession,
  } = useAipaStore();

  useEffect(() => {
    void fetchSessions();
    const interval = setInterval(() => void fetchSessions(), 10000);
    return () => clearInterval(interval);
  }, [fetchSessions]);

  if (loadingSessions && sessions.length === 0) {
    return (
      <div style={{ padding: "1rem", textAlign: "center", color: "#57606a" }}>
        載入中...
      </div>
    );
  }

  return (
    <div style={{ padding: "1rem", borderRight: "1px solid #e5e7eb" }}>
      <div style={{ marginBottom: "1rem" }}>
        <h2 style={{ fontSize: "1.125rem", fontWeight: 600, margin: 0 }}>
          🎯 工作階段
        </h2>
        <p style={{ fontSize: "0.875rem", color: "#57606a", margin: "0.5rem 0 0" }}>
          共 {sessions.length} 個
        </p>
      </div>

      {sessions.length === 0 ? (
        <div
          style={{
            background: "#f7f8fa",
            border: "1px solid #e5e7eb",
            borderRadius: "4px",
            padding: "1rem",
            textAlign: "center",
            color: "#57606a",
            fontSize: "0.875rem",
          }}
        >
          尚無工作階段
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
          {sessions.map((session) => (
            <div
              key={session.sessionId}
              onClick={() => setSelectedSession(session.sessionId)}
              style={{
                padding: "0.75rem",
                background:
                  selectedSessionId === session.sessionId
                    ? "#f0f4ff"
                    : "#ffffff",
                border:
                  selectedSessionId === session.sessionId
                    ? "1px solid #0969da"
                    : "1px solid #e5e7eb",
                borderRadius: "4px",
                cursor: "pointer",
                transition: "all 0.2s",
              }}
              onMouseEnter={(e) => {
                if (selectedSessionId !== session.sessionId) {
                  (e.currentTarget as HTMLDivElement).style.background = "#f5f5f5";
                }
              }}
              onMouseLeave={(e) => {
                if (selectedSessionId !== session.sessionId) {
                  (e.currentTarget as HTMLDivElement).style.background = "#ffffff";
                }
              }}
            >
              <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                {session.sessionId}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#57606a", marginTop: "0.25rem" }}>
                狀態: {session.status}
              </div>
              {session.requirement && (
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#57606a",
                    marginTop: "0.25rem",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                  }}
                >
                  {session.requirement}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

