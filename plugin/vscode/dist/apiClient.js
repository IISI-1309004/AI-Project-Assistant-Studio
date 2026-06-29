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
exports.AipaApiClient = void 0;
const vscode = __importStar(require("vscode"));
class AipaApiClient {
    constructor() {
        this.configuration = vscode.workspace.getConfiguration("aipa");
    }
    get baseUrl() {
        const configured = this.configuration.get("runtimeUrl", "http://localhost:8080");
        return configured.replace(/\/$/, "");
    }
    async listSessions() {
        return this.getJson("/api/v1/session");
    }
    async listCheckpoints() {
        return this.getJson("/api/v1/checkpoint");
    }
    async createSession(requirement) {
        return this.postJson("/api/v1/session", {
            requirement,
            projectId: "vscode",
            projectRoot: vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd()
        });
    }
    async approveCheckpoint(checkpointId) {
        await this.postJson(`/api/v1/checkpoint/${checkpointId}/approve`, {
            reviewer: "vscode-plugin",
            comment: "Approved from VSCode extension"
        });
    }
    async rejectCheckpoint(checkpointId) {
        await this.postJson(`/api/v1/checkpoint/${checkpointId}/reject`, {
            reviewer: "vscode-plugin",
            comment: "Rejected from VSCode extension"
        });
    }
    async getJson(path) {
        const response = await fetch(`${this.baseUrl}${path}`);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status} from ${path}`);
        }
        return (await response.json());
    }
    async postJson(path, body) {
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
        return (await response.json());
    }
}
exports.AipaApiClient = AipaApiClient;
//# sourceMappingURL=apiClient.js.map