import * as vscode from "vscode";

/**
 * AIPA Studio VSCode Extension — Phase 1 骨架
 * Phase 7 實作完整功能
 */
export function activate(context: vscode.ExtensionContext) {
  console.log("AIPA Studio extension activated (Phase 1 skeleton)");

  const askCmd = vscode.commands.registerCommand("aipa.ask", () => {
    vscode.window.showInformationMessage(
      "AIPA Studio: Phase 7 will implement full Ask AIPA functionality"
    );
  });

  context.subscriptions.push(askCmd);
}

export function deactivate() {}
