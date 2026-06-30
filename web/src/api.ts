import axios from "axios";

/**
 * 會話資訊
 */
export interface SessionItem {
  sessionId: string;
  status: string;
  requirement?: string;
  createdAt?: string;
}

/**
 * 檢查點資訊
 */
export interface CheckpointItem {
  checkpointId: string;
  sessionId?: string;
  type?: string;
  status?: string;
  spec?: string;
  diff?: string;
}

/**
 * API 客戶端
 */
const API_BASE = (import.meta.env.VITE_API_BASE as string) || "http://localhost:8080";

const client = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
});

export const apiClient = {
  // Session API
  async listSessions(): Promise<SessionItem[]> {
    try {
      const response = await client.get("/api/v1/session");
      return response.data || [];
    } catch {
      return [];
    }
  },

  async getSession(sessionId: string): Promise<SessionItem | null> {
    try {
      const response = await client.get(`/api/v1/session/${sessionId}`);
      return response.data || null;
    } catch {
      return null;
    }
  },

  // Checkpoint API
  async listCheckpoints(): Promise<CheckpointItem[]> {
    try {
      const response = await client.get("/api/v1/checkpoint");
      return response.data || [];
    } catch {
      return [];
    }
  },

  async getCheckpoint(checkpointId: string): Promise<CheckpointItem | null> {
    try {
      const response = await client.get(`/api/v1/checkpoint/${checkpointId}`);
      return response.data || null;
    } catch {
      return null;
    }
  },

  async approveCheckpoint(checkpointId: string, comment: string = ""): Promise<void> {
    await client.post(`/api/v1/checkpoint/${checkpointId}/approve`, {
      reviewer: "web-dashboard",
      comment: comment || "已從 Web Dashboard 核准",
    });
  },

  async rejectCheckpoint(checkpointId: string, comment: string = ""): Promise<void> {
    await client.post(`/api/v1/checkpoint/${checkpointId}/reject`, {
      reviewer: "web-dashboard",
      comment: comment || "已從 Web Dashboard 拒絕",
    });
  },

  // 系統狀態
  async getHealth(): Promise<{ status: string; uptime?: number }> {
    try {
      const response = await client.get("/api/v1/health");
      return response.data || { status: "離線" };
    } catch {
      return { status: "離線" };
    }
  },
};

