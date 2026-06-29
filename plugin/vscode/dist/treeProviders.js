"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.CheckpointsProvider = exports.SessionsProvider = exports.CheckpointNode = exports.SessionNode = void 0;
const vscode = __importStar(require("vscode"));
class SessionNode extends vscode.TreeItem {
    constructor(session) {
        super(`${session.sessionId}`, vscode.TreeItemCollapsibleState.None);
        this.session = session;
        this.description = session.status;
        this.tooltip = session.requirement
            ? `${session.status} - ${session.requirement}`
            : session.status;
        this.iconPath = new vscode.ThemeIcon("circle-filled");
        this.contextValue = "sessionItem";
    }
}
exports.SessionNode = SessionNode;
class CheckpointNode extends vscode.TreeItem {
    constructor(checkpoint) {
        super(`${checkpoint.type ?? "檢查點"} - ${checkpoint.checkpointId}`, vscode.TreeItemCollapsibleState.None);
        this.checkpoint = checkpoint;
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
    translateStatus(status) {
        const map = {
            "PENDING": "待審核",
            "APPROVED": "已核准",
            "REJECTED": "已拒絕"
        };
        return map[status] || status;
    }
}
exports.CheckpointNode = CheckpointNode;
class SessionsProvider {
    constructor(apiClient) {
        this.apiClient = apiClient;
        this.onDidChangeTreeDataEmitter = new vscode.EventEmitter();
        this.onDidChangeTreeData = this.onDidChangeTreeDataEmitter.event;
        this.sessions = [];
    }
    async refresh() {
        this.sessions = await this.apiClient.listSessions();
        this.onDidChangeTreeDataEmitter.fire(undefined);
    }
    getTreeItem(element) {
        return element;
    }
    getChildren() {
        return this.sessions.map((session) => new SessionNode(session));
    }
    getLatestSession() {
        return this.sessions[0];
    }
}
exports.SessionsProvider = SessionsProvider;
class CheckpointsProvider {
    constructor(apiClient) {
        this.apiClient = apiClient;
        this.onDidChangeTreeDataEmitter = new vscode.EventEmitter();
        this.onDidChangeTreeData = this.onDidChangeTreeDataEmitter.event;
        this.checkpoints = [];
    }
    async refresh() {
        this.checkpoints = await this.apiClient.listCheckpoints();
        this.onDidChangeTreeDataEmitter.fire(undefined);
    }
    getTreeItem(element) {
        return element;
    }
    getChildren() {
        return this.checkpoints.map((checkpoint) => new CheckpointNode(checkpoint));
    }
    getCheckpointIds() {
        return this.checkpoints.map((checkpoint) => checkpoint.checkpointId);
    }
    getPendingCheckpoints() {
        return this.checkpoints.filter((checkpoint) => (checkpoint.status ?? "PENDING").toUpperCase() === "PENDING");
    }
    getById(checkpointId) {
        return this.checkpoints.find((item) => item.checkpointId === checkpointId);
    }
}
exports.CheckpointsProvider = CheckpointsProvider;
//# sourceMappingURL=treeProviders.js.map