import * as vscode from "vscode";
import { AipaApiClient } from "./apiClient";
import { CheckpointNode, CheckpointsProvider, SessionsProvider } from "./treeProviders";

/**
 * AIPA Studio VSCode 擴充功能 — Phase 1 骨架
 * Phase 7 實作完整功能
 */
export function activate(context: vscode.ExtensionContext) {
  const apiClient = new AipaApiClient();
  const sessionsProvider = new SessionsProvider(apiClient);
  const checkpointsProvider = new CheckpointsProvider(apiClient);
  const seenCheckpointIds = new Set<string>();

  const statusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
  statusBar.command = "aipa.refresh";
  statusBar.text = "$(hubot) AIPA: 連線中";
  statusBar.tooltip = "AIPA 執行環境狀態";
  statusBar.show();

  const safeRefresh = async (): Promise<void> => {
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
     } catch (error) {
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
     } catch (error) {
       vscode.window.showErrorMessage(`AIPA 詢問失敗：${String(error)}`);
     }
   });

   const refreshCmd = vscode.commands.registerCommand("aipa.refresh", async () => {
     await safeRefresh();
   });

   const approveCmd = vscode.commands.registerCommand("aipa.checkpoint.approve", async (node?: CheckpointNode) => {
     if (!node?.checkpoint?.checkpointId) {
       vscode.window.showWarningMessage("請選擇要核准的檢查點。");
       return;
     }
     try {
       await apiClient.approveCheckpoint(node.checkpoint.checkpointId);
       vscode.window.showInformationMessage(`已核准 ${node.checkpoint.checkpointId}`);
       await safeRefresh();
     } catch (error) {
       vscode.window.showErrorMessage(`核准失敗：${String(error)}`);
     }
   });

   const rejectCmd = vscode.commands.registerCommand("aipa.checkpoint.reject", async (node?: CheckpointNode) => {
     if (!node?.checkpoint?.checkpointId) {
       vscode.window.showWarningMessage("請選擇要拒絕的檢查點。");
       return;
     }
     try {
       await apiClient.rejectCheckpoint(node.checkpoint.checkpointId);
       vscode.window.showInformationMessage(`已拒絕 ${node.checkpoint.checkpointId}`);
       await safeRefresh();
     } catch (error) {
       vscode.window.showErrorMessage(`拒絕失敗：${String(error)}`);
     }
   });

  context.subscriptions.push(
    vscode.window.registerTreeDataProvider("aipaSessionsView", sessionsProvider),
    vscode.window.registerTreeDataProvider("aipaCheckpointsView", checkpointsProvider),
    statusBar,
    askCmd,
    refreshCmd,
    approveCmd,
    rejectCmd
  );

  void safeRefresh();

  const intervalSeconds = vscode.workspace.getConfiguration("aipa").get<number>("pollIntervalSeconds", 10);
  const intervalHandle = setInterval(() => {
    void safeRefresh();
  }, Math.max(5, intervalSeconds) * 1000);

  context.subscriptions.push(
    new vscode.Disposable(() => {
      clearInterval(intervalHandle);
    })
  );
}

export function deactivate() {}

function getSelectedEditorText(): string {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.selection.isEmpty) {
    return "";
  }
  return editor.document.getText(editor.selection).trim();
}

async function notifyNewCheckpoint(
  checkpointId: string,
  checkpointsProvider: CheckpointsProvider,
  apiClient: AipaApiClient,
  safeRefresh: () => Promise<void>
): Promise<void> {
  const checkpoint = checkpointsProvider.getById(checkpointId);
  if (!checkpoint) {
    return;
  }

  const decision = await vscode.window.showInformationMessage(
    `AIPA 檢查點待審核：${checkpoint.type ?? "檢查點"}`,
    "核准",
    "拒絕",
    "開啟面板"
  );

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
  } catch (error) {
    vscode.window.showErrorMessage(`檢查點操作失敗：${String(error)}`);
  }
}

