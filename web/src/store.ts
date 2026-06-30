import { create } from "zustand";
import { apiClient, SessionItem, CheckpointItem } from "./api";

interface AipaStore {
  // 會話狀態
  sessions: SessionItem[];
  selectedSessionId: string | null;
  loadingSessions: boolean;

  // 檢查點狀態
  checkpoints: CheckpointItem[];
  selectedCheckpointId: string | null;
  loadingCheckpoints: boolean;

  // 系統狀態
  runtimeStatus: string;
  loadingHealth: boolean;

  // 操作
  fetchSessions: () => Promise<void>;
  fetchCheckpoints: () => Promise<void>;
  fetchHealth: () => Promise<void>;
  setSelectedSession: (sessionId: string | null) => void;
  setSelectedCheckpoint: (checkpointId: string | null) => void;
  approveCheckpoint: (checkpointId: string) => Promise<void>;
  rejectCheckpoint: (checkpointId: string) => Promise<void>;
}

export const useAipaStore = create<AipaStore>((set) => ({
  sessions: [],
  selectedSessionId: null,
  loadingSessions: false,

  checkpoints: [],
  selectedCheckpointId: null,
  loadingCheckpoints: false,

  runtimeStatus: "待連線",
  loadingHealth: false,

  fetchSessions: async () => {
    set({ loadingSessions: true });
    try {
      const sessions = await apiClient.listSessions();
      set({ sessions, loadingSessions: false });
    } catch {
      set({ loadingSessions: false });
    }
  },

  fetchCheckpoints: async () => {
    set({ loadingCheckpoints: true });
    try {
      const checkpoints = await apiClient.listCheckpoints();
      set({ checkpoints, loadingCheckpoints: false });
    } catch {
      set({ loadingCheckpoints: false });
    }
  },

  fetchHealth: async () => {
    set({ loadingHealth: true });
    try {
      const health = await apiClient.getHealth();
      set({
        runtimeStatus: health.status === "ok" ? "連線中" : "離線",
        loadingHealth: false,
      });
    } catch {
      set({ runtimeStatus: "離線", loadingHealth: false });
    }
  },

  setSelectedSession: (sessionId) => set({ selectedSessionId: sessionId }),
  setSelectedCheckpoint: (checkpointId) => set({ selectedCheckpointId: checkpointId }),

  approveCheckpoint: async (checkpointId: string) => {
    await apiClient.approveCheckpoint(checkpointId);
    // 重新載入檢查點
    const { fetchCheckpoints } = useAipaStore.getState();
    await fetchCheckpoints();
  },

  rejectCheckpoint: async (checkpointId: string) => {
    await apiClient.rejectCheckpoint(checkpointId);
    const { fetchCheckpoints } = useAipaStore.getState();
    await fetchCheckpoints();
  },
}));

