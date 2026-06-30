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
exports.activate = activate;
exports.deactivate = deactivate;
const vscode = __importStar(require("vscode"));
const apiClient_1 = require("./apiClient");
const treeProviders_1 = require("./treeProviders");
/**
 * AIPA Studio VSCode 擴充功能 — Phase 1 骨架
 * Phase 7 實作完整功能
 */
function activate(context) {
    const apiClient = new apiClient_1.AipaApiClient();
    const sessionsProvider = new treeProviders_1.SessionsProvider(apiClient);
    const checkpointsProvider = new treeProviders_1.CheckpointsProvider(apiClient);
    const seenCheckpointIds = new Set();
    const statusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
    statusBar.command = "aipa.refresh";
    statusBar.text = "$(hubot) AIPA: 連線中";
    statusBar.tooltip = "AIPA 執行環境狀態";
    statusBar.show();
    const safeRefresh = async () => {
        try {
            await sessionsProvider.refresh();
            await checkpointsProvider.refresh();
            const latest = sessionsProvider.getLatestSession();
            const pendingCount = checkpointsProvider.getPendingCheckpoints().length;
            statusBar.text = latest
                ? `$(hubot) AIPA: ${latest.status} | CP:${pendingCount}`
                : `$(hubot) AIPA: idle | CP:${pendingCount}`;
            for (const checkpointId of checkpointsProvider.getCheckpointIds()) {
                if (seenCheckpointIds.has(checkpointId)) {
                    continue;
                }
                seenCheckpointIds.add(checkpointId);
                void notifyNewCheckpoint(checkpointId, checkpointsProvider, apiClient, safeRefresh);
            }
        }
        catch (error) {
            statusBar.text = "$(warning) AIPA: 執行環境離線";
            statusBar.tooltip = String(error);
        }
    };
    const askCmd = vscode.commands.registerCommand("aipa.ask", async () => {
        const selected = getSelectedEditorText();
        const input = await vscode.window.showInputBox({
            prompt: "請描述您希望 AIPA 建置的功能",
            value: selected,
            placeHolder: "例如：新增案例提醒工作流程"
        });
        if (!input) {
            return;
        }
        try {
            const session = await apiClient.createSession(input);
            vscode.window.showInformationMessage(`AIPA 工作階段已建立：${session.sessionId}`);
            await safeRefresh();
        }
        catch (error) {
            vscode.window.showErrorMessage(`AIPA 詢問失敗：${String(error)}`);
        }
    });
    const refreshCmd = vscode.commands.registerCommand("aipa.refresh", async () => {
        await safeRefresh();
    });
    const approveCmd = vscode.commands.registerCommand("aipa.checkpoint.approve", async (node) => {
        if (!node?.checkpoint?.checkpointId) {
            vscode.window.showWarningMessage("請選擇要核准的檢查點。");
            return;
        }
        try {
            await apiClient.approveCheckpoint(node.checkpoint.checkpointId);
            vscode.window.showInformationMessage(`已核准 ${node.checkpoint.checkpointId}`);
            await safeRefresh();
        }
        catch (error) {
            vscode.window.showErrorMessage(`核准失敗：${String(error)}`);
        }
    });
    const rejectCmd = vscode.commands.registerCommand("aipa.checkpoint.reject", async (node) => {
        if (!node?.checkpoint?.checkpointId) {
            vscode.window.showWarningMessage("請選擇要拒絕的檢查點。");
            return;
        }
        try {
            await apiClient.rejectCheckpoint(node.checkpoint.checkpointId);
            vscode.window.showInformationMessage(`已拒絕 ${node.checkpoint.checkpointId}`);
            await safeRefresh();
        }
        catch (error) {
            vscode.window.showErrorMessage(`拒絕失敗：${String(error)}`);
        }
    });
    context.subscriptions.push(vscode.window.registerTreeDataProvider("aipaSessionsView", sessionsProvider), vscode.window.registerTreeDataProvider("aipaCheckpointsView", checkpointsProvider), statusBar, askCmd, refreshCmd, approveCmd, rejectCmd);
    void safeRefresh();
    const intervalSeconds = vscode.workspace.getConfiguration("aipa").get("pollIntervalSeconds", 10);
    const intervalHandle = setInterval(() => {
        void safeRefresh();
    }, Math.max(5, intervalSeconds) * 1000);
    context.subscriptions.push(new vscode.Disposable(() => {
        clearInterval(intervalHandle);
    }));
}
function deactivate() { }
function getSelectedEditorText() {
    const editor = vscode.window.activeTextEditor;
    if (!editor || editor.selection.isEmpty) {
        return "";
    }
    return editor.document.getText(editor.selection).trim();
}
async function notifyNewCheckpoint(checkpointId, checkpointsProvider, apiClient, safeRefresh) {
    const checkpoint = checkpointsProvider.getById(checkpointId);
    if (!checkpoint) {
        return;
    }
    const decision = await vscode.window.showInformationMessage(`AIPA 檢查點待審核：${checkpoint.type ?? "檢查點"}`, "核准", "拒絕", "開啟面板");
    try {
        if (decision === "核准") {
            await apiClient.approveCheckpoint(checkpointId);
            vscode.window.showInformationMessage(`已核准 ${checkpointId}`);
            await safeRefresh();
            return;
        }
        if (decision === "拒絕") {
            await apiClient.rejectCheckpoint(checkpointId);
            vscode.window.showInformationMessage(`已拒絕 ${checkpointId}`);
            await safeRefresh();
            return;
        }
        if (decision === "開啟面板") {
            await vscode.commands.executeCommand("workbench.view.extension.aipaSidebar");
        }
    }
    catch (error) {
        vscode.window.showErrorMessage(`檢查點操作失敗：${String(error)}`);
    }
}
//# sourceMappingURL=extension.js.map