#!/usr/bin/env node
import { Command } from "commander";
import chalk from "chalk";
import axios, { AxiosInstance } from "axios";
import { basename, resolve } from "node:path";

type InitStatus = {
  jobId: string;
  status: string;
  progress: number;
  message: string;
  summary?: Record<string, unknown>;
};

type SessionStatus = {
  sessionId: string;
  projectId: string;
  status: string;
  requirement: string;
  specId?: string;
  currentCheckpointId?: string | null;
  confidenceScore?: number;
  message?: string;
  nmiReport?: string;
  taskPlan?: { tasks?: Array<Record<string, unknown>>; planId?: string; summary?: string } | null;
  spec?: { title?: string; content?: string } | null;
  phase4Message?: string | null;
  execution?: {
    status?: string;
    ai?: { provider?: string; model?: string };
    prPreview?: { title?: string; branch?: string };
  } | null;
  memoryReinforcement?: {
    enabled?: boolean;
    attempted?: number;
    reinforced?: number;
    failed?: number;
  } | null;
};

const RUNTIME_URL = process.env.AIPA_RUNTIME_URL ?? "http://localhost:18080";
const http: AxiosInstance = axios.create({
  baseURL: RUNTIME_URL,
  timeout: 10000,
});

const sleep = (ms: number) => new Promise((resolveTimer) => setTimeout(resolveTimer, ms));

function normalizeProjectId(input: string): string {
  return input.toLowerCase().replace(/[^a-z0-9-_]+/g, "-").replace(/^-+|-+$/g, "") || "default";
}

async function waitForInit(jobId: string): Promise<InitStatus> {
  let latest: InitStatus = {
    jobId,
    status: "STARTED",
    progress: 0,
    message: "Waiting for initialization",
  };

  while (true) {
    const { data } = await http.get<InitStatus>(`/api/v1/project/init/${jobId}/status`);
    latest = data;
    process.stdout.write(`\r${chalk.cyan("[init]")} ${latest.progress}% ${latest.message.padEnd(48)}`);

    if (latest.status === "COMPLETED" || latest.status === "FAILED") {
      process.stdout.write("\n");
      return latest;
    }
    await sleep(1200);
  }
}

const program = new Command();

program
  .name("aipa")
  .description("AIPA Studio — AI Project Assistant Studio CLI")
  .version("1.0.0-SNAPSHOT");

// ── init ────────────────────────────────────────────────────────
program
  .command("init")
  .description("初始化專案，建立知識庫與 Project DNA")
  .option("--project-root <path>", "掃描目錄", process.cwd())
  .option("--project-id <id>", "專案識別碼")
  .option("--no-wait", "只送出 job，不等待完成")
  .action(async (opts: { projectRoot: string; projectId?: string; wait: boolean }) => {
    const projectRoot = resolve(opts.projectRoot);
    const projectId = opts.projectId ?? normalizeProjectId(basename(projectRoot));

    const { data } = await http.post("/api/v1/project/init", {
      projectRoot,
      projectId,
    });

    console.log(chalk.green(`Init job started: ${data.jobId}`));
    console.log(chalk.gray(`Runtime: ${RUNTIME_URL}`));
    console.log(chalk.gray(`Project: ${projectId} @ ${projectRoot}`));

    if (!opts.wait) {
      console.log(chalk.yellow(`Use: aipa init-status ${data.jobId}`));
      return;
    }

    const final = await waitForInit(data.jobId);
    if (final.status === "COMPLETED") {
      console.log(chalk.green("Project init completed."));
      if (final.summary) {
        console.log(chalk.gray(JSON.stringify(final.summary, null, 2)));
      }
      return;
    }

    console.error(chalk.red(`Project init failed: ${final.message}`));
    process.exitCode = 1;
  });

program
  .command("init-status <jobId>")
  .description("查詢 init job 狀態")
  .action(async (jobId: string) => {
    const { data } = await http.get<InitStatus>(`/api/v1/project/init/${jobId}/status`);
    console.log(JSON.stringify(data, null, 2));
  });

// ── ask ─────────────────────────────────────────────────────────
program
  .command("ask <requirement>")
  .description("產生規格、信心評估與 Task Planning（Phase 3 MVP）")
  .option("--project-id <id>", "專案識別碼", "default")
  .option("--project-root <path>", "專案路徑", process.cwd())
  .action(async (requirement: string, opts: { projectId: string; projectRoot: string }) => {
    const { data } = await http.post<SessionStatus>("/api/v1/session", {
      requirement,
      projectId: opts.projectId,
      projectRoot: resolve(opts.projectRoot),
    });

    console.log(chalk.green(`Session created: ${data.sessionId}`));
    console.log(chalk.cyan(`Status: ${data.status}`));
    console.log(chalk.gray(`Spec: ${data.specId ?? "N/A"}`));
    console.log(chalk.gray(`Checkpoint: ${data.currentCheckpointId ?? "N/A"}`));
    console.log(chalk.gray(`Confidence: ${data.confidenceScore ?? 0}`));
    if (data.spec?.title) {
      console.log(chalk.white(`Spec Title: ${data.spec.title}`));
    }
    if (data.message) {
      console.log(chalk.yellow(data.message));
    }
    console.log(chalk.gray(`Next: aipa checkpoint list --session-id ${data.sessionId}`));
  });

// ── scan ────────────────────────────────────────────────────────
program
  .command("scan")
  .description("重新掃描專案")
  .option("--target <path>", "指定掃描目標路徑", process.cwd())
  .option("--project-id <id>", "專案識別碼")
  .action(async (opts: { target: string; projectId?: string }) => {
    const target = resolve(opts.target);
    const projectId = opts.projectId ?? normalizeProjectId(basename(target));
    const { data } = await http.post("/api/v1/project/init", {
      projectRoot: target,
      projectId,
      fullRescan: true,
    });
    console.log(chalk.green(`Rescan job started: ${data.jobId}`));
    const final = await waitForInit(data.jobId);
    if (final.status !== "COMPLETED") {
      process.exitCode = 1;
    }
  });

// ── learn ───────────────────────────────────────────────────────
program
  .command("learn")
  .description("手動觸發學習（分析最新 PR）")
  .option("--pr <id>", "指定 PR ID")
  .option("--project-id <id>", "專案識別碼", "default")
  .option("--session-id <id>", "來源 Session ID", "")
  .option("--summary <text>", "學習摘要", "Merged PR learning")
  .option("--files <items>", "變更檔案（逗號分隔）", "")
  .option("--review-comments <items>", "審查建議（逗號分隔）", "")
  .option("--auto", "自動從最近完成的 Session 觸發學習")
  .action(async (opts: { pr?: string; projectId: string; sessionId: string; summary: string; files: string; reviewComments: string; auto?: boolean }) => {
    // Phase 5-2: Auto-trigger learning from latest COMPLETED session
    if (opts.auto) {
      try {
        const sessions = await http.get<SessionStatus[]>("/api/v1/session");
        if (!Array.isArray(sessions.data) || sessions.data.length === 0) {
          console.log(chalk.yellow("No sessions found for auto-learning trigger."));
          return;
        }

        const completedSession = sessions.data.find(s => s.status === "COMPLETED");
        if (!completedSession) {
          console.log(chalk.yellow("No completed sessions found. Use 'aipa learn' with manual options."));
          return;
        }

        console.log(chalk.cyan(`Auto-learning from session: ${completedSession.sessionId}`));
        const changedFiles = (completedSession.execution?.ai as Record<string, unknown>)?.generatedFiles as string[] ?? [];
        const learningData = {
          project_id: completedSession.projectId,
          pr_id: "auto-" + completedSession.sessionId,
          session_id: completedSession.sessionId,
          summary: `Completed: ${completedSession.spec?.title ?? "Feature"}`,
          changed_files: changedFiles,
          review_comments: ["AI-generated implementation", `Status: ${completedSession.status}`],
        };

        const { data } = await http.post("/api/v1/learn", learningData);
        console.log(chalk.green(`Auto-Learning triggered: ${data.learning_id}`));
        console.log(chalk.gray(`+knowledge: ${data.new_knowledge_count}, +memory: ${data.new_memory_count}`));
        console.log(chalk.gray(`Use: aipa learn-result ${data.learning_id}`));
      } catch (err) {
        console.error(chalk.red(`Auto-learning failed: ${err instanceof Error ? err.message : String(err)}`));
        process.exitCode = 1;
      }
      return;
    }

    const changedFiles = opts.files ? opts.files.split(",").map((item) => item.trim()).filter(Boolean) : [];
    const reviewComments = opts.reviewComments ? opts.reviewComments.split(",").map((item) => item.trim()).filter(Boolean) : [];
    const { data } = await http.post("/api/v1/learn", {
      project_id: opts.projectId,
      pr_id: opts.pr ?? "latest",
      session_id: opts.sessionId,
      summary: opts.summary,
      changed_files: changedFiles,
      review_comments: reviewComments,
    });

    console.log(chalk.green(`Learning done: ${data.learning_id}`));
    console.log(chalk.gray(`+knowledge: ${data.new_knowledge_count}, +memory: ${data.new_memory_count}`));
    console.log(chalk.gray(`Use: aipa learn-result ${data.learning_id}`));
  });

program
  .command("learn-result <learningId>")
  .description("查詢學習結果")
  .action(async (learningId: string) => {
    const { data } = await http.get(`/api/v1/learn/${learningId}`);
    console.log(JSON.stringify(data, null, 2));
  });

/**
 * Phase 5-2: Learning progress view
 */
program
  .command("learn-progress <learningId>")
  .description("查詢學習進度")
  .action(async (learningId: string) => {
    try {
      const { data } = await http.get(`/api/v1/learn/${learningId}`);
      const status = data.status ?? "UNKNOWN";
      const summary = data.summary ?? "";
      const progress = data.progress ?? 0;

      const statusColor =
        status === "COMPLETED" ? chalk.green :
        status === "FAILED" ? chalk.red :
        status === "IN_PROGRESS" ? chalk.cyan : chalk.yellow;

      console.log(chalk.cyan(`Learning Progress [${learningId}]`));
      console.log(`Status: ${statusColor(status)}`);
      console.log(`Progress: ${progress}%`);
      if (summary) {
        console.log(`Summary: ${summary}`);
      }
      if (data.new_knowledge_count) {
        console.log(chalk.green(`✓ New Knowledge: ${data.new_knowledge_count}`));
      }
      if (data.new_memory_count) {
        console.log(chalk.green(`✓ New Memory: ${data.new_memory_count}`));
      }
      if (status === "FAILED" && data.error) {
        console.log(chalk.red(`Error: ${data.error}`));
      }
    } catch (err) {
      console.error(chalk.red(`Failed to get learning progress: ${err instanceof Error ? err.message : String(err)}`));
      process.exitCode = 1;
    }
  });

program
  .command("learn-rollback <learningId>")
  .description("回滾學習結果")
  .action(async (learningId: string) => {
    const { data } = await http.post(`/api/v1/learn/${learningId}/rollback`, {});
    console.log(JSON.stringify(data, null, 2));
  });

// ── checkpoint ──────────────────────────────────────────────────
const checkpoint = program.command("checkpoint").description("管理 Human Checkpoint");
checkpoint
  .command("list")
  .description("列出待審核的 Checkpoint")
  .option("--session-id <id>", "只列出特定 Session 的 Checkpoint")
  .action(async (opts: { sessionId?: string }) => {
    const { data } = await http.get<Array<Record<string, unknown>>>("/api/v1/checkpoint", {
      params: { sessionId: opts.sessionId },
    });
    if (!Array.isArray(data) || data.length === 0) {
      console.log(chalk.yellow("No pending checkpoints."));
      return;
    }
    for (const item of data) {
      console.log(`- ${item.checkpointId} [${item.type}] session=${item.sessionId} status=${item.status}`);
    }
  });
checkpoint
  .command("approve <id>")
  .description("核准 Checkpoint")
  .option("--comments <text>", "審核備註", "")
  .action(async (id: string, opts: { comments: string }) => {
    const { data } = await http.post<{ session: SessionStatus }>(`/api/v1/checkpoint/${id}/approve`, {
      actor: "cli",
      comments: opts.comments,
    });
    const session = data.session;
    console.log(chalk.green(`Checkpoint approved: ${id}`));
    console.log(chalk.cyan(`Session ${session.sessionId} => ${session.status}`));
    if (session.nmiReport) {
      console.log(chalk.yellow(session.nmiReport));
    }
    if (session.taskPlan?.tasks && session.taskPlan.tasks.length > 0) {
      console.log(chalk.green(`Task Plan ${session.taskPlan.planId ?? ""}`));
      for (const task of session.taskPlan.tasks) {
        console.log(`- ${task.id} ${task.title}`);
      }
    }
    if (session.phase4Message) {
      console.log(chalk.yellow(session.phase4Message));
    }
    if (session.execution) {
      console.log(chalk.green(`Execution: ${session.execution.status ?? "UNKNOWN"}`));
      if (session.execution.ai?.provider) {
        console.log(chalk.gray(`AI Provider: ${session.execution.ai.provider} (${session.execution.ai.model ?? "model-unknown"})`));
      }
      if (session.execution.prPreview?.title) {
        console.log(chalk.gray(`PR Preview: ${session.execution.prPreview.title} @ ${session.execution.prPreview.branch ?? "n/a"}`));
      }
    }
    if (session.memoryReinforcement?.enabled) {
      console.log(chalk.green(`Memory Reinforcement: ${session.memoryReinforcement.reinforced ?? 0}/${session.memoryReinforcement.attempted ?? 0}`));
      if ((session.memoryReinforcement.failed ?? 0) > 0) {
        console.log(chalk.yellow(`Memory reinforce failed count: ${session.memoryReinforcement.failed}`));
      }
    }
  });
checkpoint
  .command("reject <id>")
  .description("拒絕 Checkpoint")
  .option("--comments <text>", "拒絕原因", "")
  .action(async (id: string, opts: { comments: string }) => {
    const { data } = await http.post<{ session: SessionStatus }>(`/api/v1/checkpoint/${id}/reject`, {
      actor: "cli",
      comments: opts.comments,
    });
    console.log(chalk.red(`Checkpoint rejected: ${id}`));
    console.log(chalk.cyan(`Session ${data.session.sessionId} => ${data.session.status}`));
  });

// ── health ──────────────────────────────────────────────────────
program
  .command("health")
  .description("全系統健康檢查")
  .action(async () => {
    try {
      const res = await http.get("/api/v1/health", { timeout: 3000 });
      console.log(chalk.green("✅ Runtime Service: UP"));
      console.log(chalk.gray(JSON.stringify(res.data, null, 2)));
    } catch {
      console.log(chalk.red("❌ Runtime Service: 無回應（請執行 aipa server start）"));
    }
  });

// ── knowledge ───────────────────────────────────────────────────
const knowledge = program.command("knowledge").description("知識庫查詢");
const memory = program.command("memory").description("記憶查詢");

knowledge
  .command("search <query>")
  .description("搜尋知識庫")
  .option("--project-id <id>", "專案識別碼", "default")
  .option("--top-k <n>", "回傳筆數", "5")
  .action(async (query: string, opts: { projectId: string; topK: string }) => {
    const { data } = await http.post("/api/v1/knowledge/search", {
      query,
      projectId: opts.projectId,
      topK: Number(opts.topK),
    });

    if (!Array.isArray(data) || data.length === 0) {
      console.log(chalk.yellow("No knowledge found."));
      return;
    }
    for (const item of data) {
      console.log(`- ${item.title} [${item.category}] (score=${(item._score ?? 0).toFixed?.(3) ?? item._score ?? 0})`);
    }
  });

memory
  .command("list")
  .description("列出記憶條目")
  .requiredOption("--project-id <id>", "專案識別碼")
  .option("--type <name>", "記憶類型")
  .action(async (opts: { projectId: string; type?: string }) => {
    const { data } = await http.get("/api/v1/memory", {
      params: {
        projectId: opts.projectId,
        type: opts.type,
      },
    });
    if (!Array.isArray(data) || data.length === 0) {
      console.log(chalk.yellow("Memory is empty."));
      return;
    }
    for (const item of data) {
      console.log(`- ${item.id} ${item.type} ${item.key} (strength=${item.strength})`);
    }
  });

memory
  .command("show <id>")
  .description("顯示單一記憶")
  .action(async (id: string) => {
    const { data } = await http.get(`/api/v1/memory/${id}`);
    console.log(JSON.stringify(data, null, 2));
  });

memory
  .command("reinforce <id>")
  .description("強化記憶")
  .action(async (id: string) => {
    const { data } = await http.post(`/api/v1/memory/${id}/reinforce`, {});
    console.log(JSON.stringify(data, null, 2));
  });

knowledge
  .command("list")
  .description("列出知識項目")
  .requiredOption("--project-id <id>", "專案識別碼")
  .option("--category <name>", "分類過濾")
  .action(async (opts: { projectId: string; category?: string }) => {
    const { data } = await http.get("/api/v1/knowledge", {
      params: {
        projectId: opts.projectId,
        category: opts.category,
      },
    });

    if (!Array.isArray(data) || data.length === 0) {
      console.log(chalk.yellow("Knowledge base is empty."));
      return;
    }
    for (const item of data) {
      console.log(`- ${item.id} ${item.title} [${item.category}]`);
    }
  });

// ── version ─────────────────────────────────────────────────────
program
  .command("version")
  .description("顯示版本資訊")
  .action(() => {
    console.log(chalk.cyan("AIPA Studio CLI v1.0.0-SNAPSHOT (Phase 5-2 — Auto-Learning)"));
  });

// ── status ──────────────────────────────────────────────────────
program
  .command("status")
  .description("顯示目前 Session 狀態")
  .argument("[sessionId]", "Session ID")
  .action(async (sessionId?: string) => {
    if (sessionId) {
      const { data } = await http.get<SessionStatus>(`/api/v1/session/${sessionId}`);
      console.log(JSON.stringify(data, null, 2));
      return;
    }

    const { data } = await http.get<SessionStatus[]>("/api/v1/session");
    if (!Array.isArray(data) || data.length === 0) {
      console.log(chalk.yellow("No sessions found."));
      return;
    }
    const latest = data[0];
    console.log(JSON.stringify(latest, null, 2));
  });

// ── server ──────────────────────────────────────────────────────
const server = program.command("server").description("管理 Runtime Service");
server.command("start").action(() => console.log(chalk.yellow("⚠️  Phase 1: use 'make docker-up' for now")));
server.command("stop").action(() => console.log(chalk.yellow("⚠️  Phase 1: use 'make docker-down' for now")));
server.command("status").action(async () => {
  try {
    const res = await http.get("/api/v1/health", { timeout: 3000 });
    console.log(chalk.green(`✅ Runtime Service is UP — version ${res.data.version}`));
  } catch {
    console.log(chalk.red("❌ Runtime Service is DOWN"));
  }
});

program.parseAsync().catch((err: unknown) => {
  const message = err instanceof Error ? err.message : String(err);
  console.error(chalk.red(`Command failed: ${message}`));
  process.exitCode = 1;
});
