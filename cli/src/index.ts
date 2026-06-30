#!/usr/bin/env node
import { Command } from "commander";
import chalk from "chalk";
import axios, { AxiosInstance } from "axios";
import { basename, resolve } from "node:path";
import { access, mkdir, readFile, writeFile } from "node:fs/promises";
import { constants as fsConstants } from "node:fs";
import { spawn } from "node:child_process";
import { config as loadEnv } from "dotenv";
import { parseExcludePatterns, redactSensitiveText } from "./security";

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

// Load local env files when running CLI directly from a workspace.
loadEnv({ path: resolve(process.cwd(), ".env.local"), override: false });
loadEnv({ path: resolve(process.cwd(), ".env"), override: false });

const RUNTIME_URL = process.env.AIPA_RUNTIME_URL ?? "http://localhost:18080";
const HTTP_TIMEOUT_MS = Number(process.env.AIPA_HTTP_TIMEOUT_MS ?? "60000");
const http: AxiosInstance = axios.create({
  baseURL: RUNTIME_URL,
  timeout: Number.isFinite(HTTP_TIMEOUT_MS) && HTTP_TIMEOUT_MS > 0 ? HTTP_TIMEOUT_MS : 60000,
});

const sleep = (ms: number) => new Promise((resolveTimer) => setTimeout(resolveTimer, ms));

type DoctorCheckResult = {
  name: string;
  status: "PASS" | "WARN" | "FAIL";
  detail: string;
  hint?: string;
};


function isNodeVersionSupported(versionText: string): boolean {
  const major = Number(versionText.replace(/^v/, "").split(".")[0]);
  return Number.isFinite(major) && major >= 20;
}

function normalizeProjectId(input: string): string {
  return input.toLowerCase().replace(/[^a-z0-9-_]+/g, "-").replace(/^-+|-+$/g, "") || "default";
}

const PROJECT_META_RELATIVE_PATH = ".ai-project/project.json";

type ProjectMeta = {
  projectId: string;
  projectRoot: string;
  updatedAt: string;
};

function rewriteSlashCommandArgs(argv: string[]): string[] {
  const [nodeBin, scriptPath, maybeSlash, ...rest] = argv;
  if (!maybeSlash || !maybeSlash.startsWith("/")) {
    return argv;
  }

  const slash = maybeSlash.toLowerCase();
  const mapped = (() => {
    switch (slash) {
      case "/spec":
        return ["ask", ...rest];
      case "/plan":
        return ["checkpoint", "list", ...rest];
      case "/approve":
      case "/do":
      case "/pr":
        return ["checkpoint", "approve", ...rest];
      case "/reject":
        return ["checkpoint", "reject", ...rest];
      case "/learn":
        return rest.length > 0 ? ["learn", ...rest] : ["learn", "--auto"];
      case "/test":
        return ["test-local", ...rest];
      default:
        return [maybeSlash, ...rest];
    }
  })();

  return [nodeBin, scriptPath, ...mapped];
}

process.argv = rewriteSlashCommandArgs(process.argv);

async function readProjectMeta(projectRoot: string): Promise<ProjectMeta | null> {
  try {
    const text = await readFile(resolve(projectRoot, PROJECT_META_RELATIVE_PATH), "utf-8");
    const parsed = JSON.parse(text) as Partial<ProjectMeta>;
    if (!parsed.projectId) {
      return null;
    }
    return {
      projectId: parsed.projectId,
      projectRoot: parsed.projectRoot ?? projectRoot,
      updatedAt: parsed.updatedAt ?? new Date().toISOString(),
    };
  } catch {
    return null;
  }
}

async function writeProjectMeta(projectRoot: string, projectId: string): Promise<void> {
  const aiProjectDir = resolve(projectRoot, ".ai-project");
  await mkdir(aiProjectDir, { recursive: true });
  const payload: ProjectMeta = {
    projectId,
    projectRoot,
    updatedAt: new Date().toISOString(),
  };
  await writeFile(resolve(projectRoot, PROJECT_META_RELATIVE_PATH), JSON.stringify(payload, null, 2), "utf-8");
}

async function resolveProjectId(
  explicitProjectId: string | undefined,
  projectRootInput: string | undefined
): Promise<string> {
  if (explicitProjectId?.trim()) {
    return normalizeProjectId(explicitProjectId);
  }

  if (process.env.AIPA_PROJECT_ID?.trim()) {
    return normalizeProjectId(process.env.AIPA_PROJECT_ID);
  }

  const projectRoot = resolve(projectRootInput ?? process.cwd());
  const meta = await readProjectMeta(projectRoot);
  if (meta?.projectId) {
    return normalizeProjectId(meta.projectId);
  }

  return normalizeProjectId(basename(projectRoot));
}

function runCommandForCurrentProject(command: string, args: string[]): Promise<number> {
  return new Promise<number>((resolveCode, rejectCode) => {
    const child = spawn(command, args, {
      stdio: "inherit",
      shell: process.platform === "win32",
    });
    child.on("error", rejectCode);
    child.on("close", (code) => resolveCode(code ?? 1));
  });
}

async function runLocalTestWrapper(): Promise<number> {
  const cwd = process.cwd();

  try {
    await access(resolve(cwd, "gradlew.bat"), fsConstants.X_OK);
    return await runCommandForCurrentProject(".\\gradlew.bat", ["test"]);
  } catch {
    // continue
  }

  try {
    await access(resolve(cwd, "gradlew"), fsConstants.X_OK);
    return await runCommandForCurrentProject("./gradlew", ["test"]);
  } catch {
    // continue
  }

  try {
    await access(resolve(cwd, "pom.xml"), fsConstants.F_OK);
    return await runCommandForCurrentProject("mvn", ["test"]);
  } catch {
    // continue
  }

  try {
    const pkgText = await readFile(resolve(cwd, "package.json"), "utf-8");
    const pkg = JSON.parse(pkgText) as { scripts?: Record<string, string> };
    if (pkg.scripts?.test) {
      return await runCommandForCurrentProject("npm", ["test"]);
    }
  } catch {
    // continue
  }

  try {
    await access(resolve(cwd, "pyproject.toml"), fsConstants.F_OK);
    return await runCommandForCurrentProject("pytest", []);
  } catch {
    // continue
  }

  try {
    await access(resolve(cwd, "requirements.txt"), fsConstants.F_OK);
    return await runCommandForCurrentProject("pytest", []);
  } catch {
    // continue
  }

  console.error(chalk.red("No known test command found in current directory."));
  console.error(chalk.gray("Expected one of: gradlew(.bat), pom.xml, package.json(test), pyproject.toml, requirements.txt"));
  return 1;
}

async function waitForInit(jobId: string): Promise<InitStatus> {
  let latest: InitStatus = {
    jobId,
    status: "STARTED",
    progress: 0,
    message: "Waiting for initialization",
  };
  let lastRenderKey = "";
  let pollTick = 0;
  const startedAt = Date.now();

  while (true) {
    const { data } = await http.get<InitStatus>(`/api/v1/project/init/${jobId}/status`);
    latest = data;
    pollTick += 1;
    const spinner = ["|", "/", "-", "\\"][pollTick % 4] ?? "|";
    const elapsedSec = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
    const renderKey = `${latest.status}|${latest.progress}|${latest.message}`;
    if (renderKey !== lastRenderKey) {
      process.stdout.write(
        `\r${chalk.cyan("[init]")} ${spinner} ${latest.progress}% ${latest.message.padEnd(56)} ${chalk.gray(`(${elapsedSec}s)`)}`,
      );
      lastRenderKey = renderKey;
    }

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
    const projectId = await resolveProjectId(opts.projectId, projectRoot);

    const { data } = await http.post("/api/v1/project/init", {
      projectRoot,
      projectId,
    });

    console.log(chalk.green(`Init job started: ${data.jobId}`));
    console.log(chalk.gray(`Runtime: ${RUNTIME_URL}`));
    console.log(chalk.gray(`Project: ${projectId} @ ${projectRoot}`));
    await writeProjectMeta(projectRoot, projectId);

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
  .option("--project-id <id>", "專案識別碼")
  .option("--project-root <path>", "專案路徑", process.cwd())
  .action(async (requirement: string, opts: { projectId?: string; projectRoot: string }) => {
    const { sanitized, redactedCount } = redactSensitiveText(requirement);
    const projectId = await resolveProjectId(opts.projectId, opts.projectRoot);
    if (redactedCount > 0) {
      console.log(chalk.yellow(`⚠️  已遮罩 ${redactedCount} 個敏感片段（contextExcludePatterns 生效）`));
    }

    const { data } = await http.post<SessionStatus>("/api/v1/session", {
      requirement: sanitized,
      projectId,
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
    const projectId = await resolveProjectId(opts.projectId, target);
    const { data } = await http.post("/api/v1/project/init", {
      projectRoot: target,
      projectId,
      fullRescan: true,
    });
    await writeProjectMeta(target, projectId);
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
  .option("--project-id <id>", "專案識別碼")
  .option("--session-id <id>", "來源 Session ID", "")
  .option("--summary <text>", "學習摘要", "Merged PR learning")
  .option("--files <items>", "變更檔案（逗號分隔）", "")
  .option("--review-comments <items>", "審查建議（逗號分隔）", "")
  .option("--auto", "自動從最近完成的 Session 觸發學習")
  .action(async (opts: { pr?: string; projectId?: string; sessionId: string; summary: string; files: string; reviewComments: string; auto?: boolean }) => {
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
    const projectId = await resolveProjectId(opts.projectId, process.cwd());
    const { data } = await http.post("/api/v1/learn", {
      project_id: projectId,
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
  .option("--project-id <id>", "專案識別碼")
  .option("--top-k <n>", "回傳筆數", "5")
  .action(async (query: string, opts: { projectId?: string; topK: string }) => {
    const projectId = await resolveProjectId(opts.projectId, process.cwd());
    const { data } = await http.post("/api/v1/knowledge/search", {
      query,
      projectId,
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
  .option("--project-id <id>", "專案識別碼")
  .option("--type <name>", "記憶類型")
  .action(async (opts: { projectId?: string; type?: string }) => {
    const projectId = await resolveProjectId(opts.projectId, process.cwd());
    const { data } = await http.get("/api/v1/memory", {
      params: {
        projectId,
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
  .option("--project-id <id>", "專案識別碼")
  .option("--category <name>", "分類過濾")
  .action(async (opts: { projectId?: string; category?: string }) => {
    const projectId = await resolveProjectId(opts.projectId, process.cwd());
    const { data } = await http.get("/api/v1/knowledge", {
      params: {
        projectId,
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
  .option("--memory", "只顯示 Session 的 memory reinforcement 狀態")
  .argument("[sessionId]", "Session ID")
  .action(async (sessionId: string | undefined, opts: { memory?: boolean }) => {
    if (sessionId && opts.memory) {
      const { data } = await http.get(`/api/v1/session/${sessionId}/memory-reinforcement`);
      console.log(JSON.stringify(data, null, 2));
      return;
    }

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

    if (opts.memory) {
      const { data: reinforcement } = await http.get(`/api/v1/session/${latest.sessionId}/memory-reinforcement`);
      console.log(JSON.stringify(reinforcement, null, 2));
      return;
    }

    console.log(JSON.stringify(latest, null, 2));
  });

program
  .command("session-memory <sessionId>")
  .description("查詢指定 Session 的 memory reinforcement 結果")
  .action(async (sessionId: string) => {
    try {
      const { data } = await http.get(`/api/v1/session/${sessionId}/memory-reinforcement`);
      console.log(JSON.stringify(data, null, 2));
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to query memory reinforcement: ${message}`));
      process.exitCode = 1;
    }
  });

program
  .command("session-summary <sessionId>")
  .description("查詢指定 Session 的 completion summary（learning + memory）")
  .action(async (sessionId: string) => {
    try {
      const { data } = await http.get(`/api/v1/session/${sessionId}/summary`);
      console.log(JSON.stringify(data, null, 2));
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to query session summary: ${message}`));
      process.exitCode = 1;
    }
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

// ── test-local (slash /test wrapper) ────────────────────────────
program
  .command("test-local")
  .description("在當前專案自動選擇測試命令（Gradle/Maven/npm/pytest）")
  .action(async () => {
    const code = await runLocalTestWrapper();
    if (code !== 0) {
      process.exitCode = code;
    }
  });

// ── wisdom (Phase 6) ────────────────────────────────────────────
const wisdom = program.command("wisdom").description("智慧規則管理（Phase 6）");

wisdom
  .command("list")
  .description("列出所有智慧規則")
  .option("--project <projectId>", "篩選特定專案的規則", "")
  .action(async (opts) => {
    try {
      const url = opts.project
        ? `/api/v1/wisdom/rules?projectId=${opts.project}`
        : "/api/v1/wisdom/rules";
      const { data } = await http.get<Array<Record<string, unknown>>>(url);
      if (!data || data.length === 0) {
        console.log(chalk.yellow("No wisdom rules found."));
        return;
      }
      data.forEach((rule) => {
        const severity = rule["severity"] as string;
        const icon = severity === "BLOCK" ? chalk.red("🚫 BLOCK") : chalk.yellow("⚠️  WARN ");
        console.log(`${icon}  [${rule["id"]}] ${rule["title"]}`);
        if (rule["description"]) {
          console.log(`        ${chalk.gray(String(rule["description"]).substring(0, 80))}`);
        }
      });
      console.log(chalk.gray(`\nTotal: ${data.length} rules`));
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to list wisdom rules: ${message}`));
      process.exitCode = 1;
    }
  });

wisdom
  .command("add")
  .description("新增智慧規則")
  .requiredOption("--title <title>", "規則標題")
  .requiredOption("--desc <description>", "規則描述")
  .option("--severity <severity>", "嚴重等級 WARN|BLOCK", "WARN")
  .option("--id <id>", "規則 ID（選填）")
  .option("--condition <condition>", "觸發條件")
  .action(async (opts) => {
    try {
      const rule: Record<string, unknown> = {
        title: opts.title,
        description: opts.desc,
        severity: opts.severity.toUpperCase(),
        scope: { global: true },
        trigger_conditions: opts.condition ? [opts.condition] : [],
        enabled: true,
      };
      if (opts.id) rule["id"] = opts.id;
      const { data } = await http.post("/api/v1/wisdom/rules", rule);
      console.log(chalk.green(`✅ Wisdom rule created: ${data["id"]}`));
      console.log(`   Title: ${data["title"]}`);
      console.log(`   Severity: ${data["severity"]}`);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to add wisdom rule: ${message}`));
      process.exitCode = 1;
    }
  });

wisdom
  .command("check")
  .description("對程式碼變更執行智慧規則檢查")
  .option("--diff <codeDiff>", "程式碼 diff 內容", "")
  .option("--files <files>", "異動檔案（逗號分隔）", "")
  .option("--type <specType>", "Spec 類型", "FEATURE")
  .action(async (opts) => {
    try {
      const redacted = redactSensitiveText(opts.diff);
      if (redacted.redactedCount > 0) {
        console.log(chalk.yellow(`⚠️  已遮罩 ${redacted.redactedCount} 個敏感片段（contextExcludePatterns 生效）`));
      }
      const context = {
        code_diff: redacted.sanitized,
        file_names: opts.files ? opts.files.split(",").map((f: string) => f.trim()) : [],
        spec_type: opts.type,
      };
      const { data } = await http.post<{
        hasBlockViolation: boolean;
        blockCount: number;
        warnCount: number;
        matchedRules: Array<Record<string, unknown>>;
      }>("/api/v1/wisdom/check", context);
      if (data.hasBlockViolation) {
        console.log(chalk.red(`🚫 BLOCK violation! (${data.blockCount} BLOCK, ${data.warnCount} WARN)`));
      } else if (data.warnCount > 0) {
        console.log(chalk.yellow(`⚠️  ${data.warnCount} WARN rule(s) matched.`));
      } else {
        console.log(chalk.green("✅ No wisdom rule violations."));
      }
      (data.matchedRules ?? []).forEach((rule) => {
        const icon = rule["severity"] === "BLOCK" ? chalk.red("🚫") : chalk.yellow("⚠️");
        console.log(`  ${icon} [${rule["id"]}] ${rule["title"]}`);
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to check wisdom rules: ${message}`));
      process.exitCode = 1;
    }
  });

// ── doctor (Phase 9) ─────────────────────────────────────────────
program
  .command("doctor")
  .description("企業強化診斷（Phase 9）：檢查 Runtime、環境、敏感設定")
  .option("--json", "以 JSON 輸出診斷結果")
  .action(async (opts: { json?: boolean }) => {
    const checks: DoctorCheckResult[] = [];

    try {
      const res = await http.get("/api/v1/health", { timeout: 3000 });
      checks.push({
        name: "runtime",
        status: "PASS",
        detail: `Runtime 可連線 (${res.status}) @ ${RUNTIME_URL}`,
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      checks.push({
        name: "runtime",
        status: "FAIL",
        detail: `Runtime 無回應 @ ${RUNTIME_URL}`,
        hint: `請先啟動服務後重試（${message}）`,
      });
    }

    checks.push({
      name: "node",
      status: isNodeVersionSupported(process.version) ? "PASS" : "WARN",
      detail: `Node.js ${process.version}`,
      hint: isNodeVersionSupported(process.version) ? undefined : "建議升級到 Node.js >= 20",
    });

    const configuredProviders = [
      "CLAUDE_API_KEY",
      "OPENAI_API_KEY",
      "GEMINI_API_KEY",
      "OLLAMA_BASE_URL",
      "GITHUB_TOKEN",
    ].filter((name) => Boolean(process.env[name]));
    checks.push({
      name: "ai-provider",
      status: configuredProviders.length > 0 ? "PASS" : "WARN",
      detail: configuredProviders.length > 0
        ? `已設定 ${configuredProviders.length} 個 AI 供應商`
        : "未偵測到 AI 供應商設定",
      hint: configuredProviders.length > 0 ? undefined : "請設定 API Key 或 Ollama URL",
    });

    try {
      await access(process.cwd(), fsConstants.W_OK);
      checks.push({
        name: "workspace-write",
        status: "PASS",
        detail: `工作目錄可寫入 (${process.cwd()})`,
      });
    } catch {
      checks.push({
        name: "workspace-write",
        status: "FAIL",
        detail: `工作目錄不可寫入 (${process.cwd()})`,
        hint: "請檢查目錄權限",
      });
    }

    const excludePatterns = parseExcludePatterns(process.env.AIPA_CONTEXT_EXCLUDE_PATTERNS);
    checks.push({
      name: "context-exclude",
      status: excludePatterns.length > 0 ? "PASS" : "WARN",
      detail: `自訂遮罩規則數量: ${excludePatterns.length}`,
      hint: excludePatterns.length > 0 ? undefined : "建議設定 AIPA_CONTEXT_EXCLUDE_PATTERNS",
    });

    if (opts.json) {
      console.log(JSON.stringify({
        runtimeUrl: RUNTIME_URL,
        timestamp: new Date().toISOString(),
        checks,
      }, null, 2));
    } else {
      console.log(chalk.cyan("AIPA Doctor Report"));
      for (const check of checks) {
        const icon = check.status === "PASS" ? chalk.green("✅") : check.status === "WARN" ? chalk.yellow("⚠️") : chalk.red("❌");
        console.log(`${icon} ${check.name}: ${check.detail}`);
        if (check.hint) {
          console.log(chalk.gray(`   hint: ${check.hint}`));
        }
      }
    }

    if (checks.some((check) => check.status === "FAIL")) {
      process.exitCode = 1;
    }
  });

// ── experience (Phase 6) ─────────────────────────────────────────
const experience = program.command("experience").description("經驗案例管理（Phase 6）");

experience
  .command("search <query>")
  .description("搜尋相似歷史案例（相似度 > 0.6）")
  .option("--project <projectId>", "限定專案 ID", "")
  .option("--top <n>", "回傳筆數", "5")
  .action(async (query: string, opts) => {
    try {
      const { data } = await http.post<Array<Record<string, unknown>>>("/api/v1/experience/search", {
        query,
        project_id: opts.project,
        top_k: parseInt(opts.top, 10),
      });
      if (!data || data.length === 0) {
        console.log(chalk.yellow("No similar cases found (similarity threshold: 0.6)."));
        return;
      }
      console.log(chalk.green(`Found ${data.length} similar case(s):\n`));
      data.forEach((c, i) => {
        const sim = typeof c["_similarity"] === "number"
          ? ` (similarity: ${(c["_similarity"] as number).toFixed(2)})`
          : "";
        console.log(`${i + 1}. [${c["id"]}] ${c["title"]}${sim}`);
        if (c["requirement"]) {
          console.log(`   ${chalk.gray(String(c["requirement"]).substring(0, 100))}`);
        }
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to search experience cases: ${message}`));
      process.exitCode = 1;
    }
  });

experience
  .command("list")
  .description("列出所有歷史案例")
  .requiredOption("--project <projectId>", "專案 ID")
  .action(async (opts) => {
    try {
      const { data } = await http.get<Array<Record<string, unknown>>>(
        `/api/v1/experience/cases?project_id=${opts.project}`
      );
      if (!data || data.length === 0) {
        console.log(chalk.yellow("No experience cases found."));
        return;
      }
      console.log(chalk.bold(`Experience Cases for '${opts.project}':\n`));
      data.forEach((c, i) => {
        const icon = c["outcome"] === "SUCCESS" ? chalk.green("✅") : chalk.yellow("⚠️");
        console.log(`${i + 1}. ${icon} [${c["id"]}] ${c["title"]}`);
        console.log(`   Created: ${c["created_at"]}  References: ${c["reference_count"] ?? 0}`);
      });
      console.log(chalk.gray(`\nTotal: ${data.length} cases`));
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(chalk.red(`Failed to list experience cases: ${message}`));
      process.exitCode = 1;
    }
  });

program.parseAsync().catch((err: unknown) => {
  const message = err instanceof Error ? err.message : String(err);
  console.error(chalk.red(`Command failed: ${message}`));
  process.exitCode = 1;
});
