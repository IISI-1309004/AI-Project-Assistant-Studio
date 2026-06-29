import * as vscode from "vscode";
import { AipaApiClient, CheckpointItem, SessionItem } from "./apiClient";

export class SessionNode extends vscode.TreeItem {
  constructor(readonly session: SessionItem) {
    super(`${session.sessionId}`, vscode.TreeItemCollapsibleState.None);
    this.description = session.status;
    this.tooltip = session.requirement
      ? `${session.status} - ${session.requirement}`
      : session.status;
    this.iconPath = new vscode.ThemeIcon("circle-filled");
    this.contextValue = "sessionItem";
  }
}

export class CheckpointNode extends vscode.TreeItem {
  constructor(readonly checkpoint: CheckpointItem) {
    super(`${checkpoint.type ?? "檢查點"} - ${checkpoint.checkpointId}`, vscode.TreeItemCollapsibleState.None);
    const status = (checkpoint.status ?? "待審核").toUpperCase();
    this.description = this.translateStatus(status);
    this.tooltip = checkpoint.sessionId
      ? `工作階段：${checkpoint.sessionId}`
      : "待審核檢查點";
    this.iconPath = status === "已核准"
      ? new vscode.ThemeIcon("pass")
      : status === "已拒絕"
        ? new vscode.ThemeIcon("error")
        : new vscode.ThemeIcon("shield");
    this.contextValue = status === "待審核" ? "checkpointPending" : "checkpointReadonly";
  }

  private translateStatus(status: string): string {
    const map: Record<string, string> = {
      "PENDING": "待審核",
      "APPROVED": "已核准",
      "REJECTED": "已拒絕"
    };
    return map[status] || status;
  }
}

export class SessionsProvider implements vscode.TreeDataProvider<SessionNode> {
  private readonly onDidChangeTreeDataEmitter = new vscode.EventEmitter<SessionNode | undefined>();
  readonly onDidChangeTreeData = this.onDidChangeTreeDataEmitter.event;
  private sessions: SessionItem[] = [];

  constructor(private readonly apiClient: AipaApiClient) {}

  async refresh(): Promise<void> {
    this.sessions = await this.apiClient.listSessions();
    this.onDidChangeTreeDataEmitter.fire(undefined);
  }

  getTreeItem(element: SessionNode): vscode.TreeItem {
    return element;
  }

  getChildren(): SessionNode[] {
    return this.sessions.map((session) => new SessionNode(session));
  }

  getLatestSession(): SessionItem | undefined {
    return this.sessions[0];
  }
}

export class CheckpointsProvider implements vscode.TreeDataProvider<CheckpointNode> {
  private readonly onDidChangeTreeDataEmitter = new vscode.EventEmitter<CheckpointNode | undefined>();
  readonly onDidChangeTreeData = this.onDidChangeTreeDataEmitter.event;
  private checkpoints: CheckpointItem[] = [];

  constructor(private readonly apiClient: AipaApiClient) {}

  async refresh(): Promise<void> {
    this.checkpoints = await this.apiClient.listCheckpoints();
    this.onDidChangeTreeDataEmitter.fire(undefined);
  }

  getTreeItem(element: CheckpointNode): vscode.TreeItem {
    return element;
  }

  getChildren(): CheckpointNode[] {
    return this.checkpoints.map((checkpoint) => new CheckpointNode(checkpoint));
  }

  getCheckpointIds(): string[] {
    return this.checkpoints.map((checkpoint) => checkpoint.checkpointId);
  }

  getPendingCheckpoints(): CheckpointItem[] {
    return this.checkpoints.filter((checkpoint) => (checkpoint.status ?? "PENDING").toUpperCase() === "PENDING");
  }

  getById(checkpointId: string): CheckpointItem | undefined {
    return this.checkpoints.find((item) => item.checkpointId === checkpointId);
  }
}

