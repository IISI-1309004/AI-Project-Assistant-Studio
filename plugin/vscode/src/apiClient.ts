import * as vscode from "vscode";

export interface SessionItem {
  sessionId: string;
  status: string;
  requirement?: string;
}

export interface CheckpointItem {
  checkpointId: string;
  sessionId?: string;
  type?: string;
  status?: string;
}

export class AipaApiClient {
  private readonly configuration = vscode.workspace.getConfiguration("aipa");

  private get baseUrl(): string {
    const configured = this.configuration.get<string>("runtimeUrl", "http://localhost:8080");
    return configured.replace(/\/$/, "");
  }

  async listSessions(): Promise<SessionItem[]> {
    return this.getJson<SessionItem[]>("/api/v1/session");
  }

  async listCheckpoints(): Promise<CheckpointItem[]> {
    return this.getJson<CheckpointItem[]>("/api/v1/checkpoint");
  }

  async createSession(requirement: string): Promise<SessionItem> {
    return this.postJson<SessionItem>("/api/v1/session", {
      requirement,
      projectId: "vscode",
      projectRoot: vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd()
    });
  }

  async approveCheckpoint(checkpointId: string): Promise<void> {
    await this.postJson(`/api/v1/checkpoint/${checkpointId}/approve`, {
      reviewer: "vscode-plugin",
      comment: "Approved from VSCode extension"
    });
  }

  async rejectCheckpoint(checkpointId: string): Promise<void> {
    await this.postJson(`/api/v1/checkpoint/${checkpointId}/reject`, {
      reviewer: "vscode-plugin",
      comment: "Rejected from VSCode extension"
    });
  }

  private async getJson<T>(path: string): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status} from ${path}`);
    }
    return (await response.json()) as T;
  }

  private async postJson<T = unknown>(path: string, body: Record<string, unknown>): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(body)
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status} from ${path}`);
    }
    return (await response.json()) as T;
  }
}

