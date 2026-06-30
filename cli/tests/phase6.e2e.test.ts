import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { createServer, IncomingMessage, ServerResponse } from "node:http";
import { spawn } from "node:child_process";

const rootDir = process.cwd();

async function runCli(args: string[], port: number): Promise<string> {
  return await new Promise<string>((resolveOutput, rejectOutput) => {
    const child = spawn("node", ["./dist/index.js", ...args], {
      cwd: rootDir,
      env: {
        ...process.env,
        AIPA_RUNTIME_URL: `http://127.0.0.1:${port}`,
        FORCE_COLOR: "0",
        NO_PROXY: "127.0.0.1,localhost",
        HTTP_PROXY: "",
        HTTPS_PROXY: "",
      },
      stdio: ["ignore", "pipe", "pipe"],
    });

    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => { stdout += chunk.toString(); });
    child.stderr.on("data", (chunk) => { stderr += chunk.toString(); });
    child.on("error", rejectOutput);
    child.on("close", (code) => {
      if (code === 0) {
        resolveOutput(stdout);
      } else {
        rejectOutput(new Error(stderr || stdout));
      }
    });
  });
}

async function runCliWithRetry(args: string[], port: number): Promise<string> {
  try {
    return await runCli(args, port);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    if (!message.includes("ETIMEDOUT")) throw err;
    return await runCli(args, port);
  }
}

describe("Phase 6 CLI wisdom and experience e2e", () => {
  let serverPort = 0;

  const server = createServer((req: IncomingMessage, res: ServerResponse) => {
    const url = req.url ?? "";
    res.setHeader("Content-Type", "application/json");

    // ── Wisdom rules ──────────────────────────────────────────────────
    if (req.method === "GET" && url.startsWith("/api/v1/wisdom/rules")) {
      res.end(JSON.stringify([
        {
          id: "WIS-JAVA-001",
          title: "禁止在 Controller 層直接注入 Repository",
          description: "Controller 層不得直接依賴 Repository",
          severity: "BLOCK",
          scope: { global: false },
          trigger_conditions: ["Controller 類別中注入了 @Repository"],
          enabled: true,
          hit_count: 0,
        },
        {
          id: "WIS-DB-001",
          title: "UPDATE/DELETE 語句必須有 WHERE 條件",
          description: "禁止無 WHERE 條件的 UPDATE 或 DELETE",
          severity: "BLOCK",
          scope: { global: true },
          trigger_conditions: ["SQL 語句中有 UPDATE 或 DELETE 但缺少 WHERE"],
          enabled: true,
          hit_count: 2,
        },
        {
          id: "WIS-JAVA-002",
          title: "不得在 for loop 內執行 DB 查詢（N+1 問題）",
          description: "禁止在迴圈內執行資料庫查詢",
          severity: "WARN",
          scope: { global: true },
          trigger_conditions: ["for 迴圈內有 repository.findById"],
          enabled: true,
          hit_count: 1,
        },
      ]));
      return;
    }

    if (req.method === "POST" && url === "/api/v1/wisdom/rules") {
      let body = "";
      req.on("data", (chunk) => { body += chunk; });
      req.on("end", () => {
        const parsed = JSON.parse(body || "{}");
        res.end(JSON.stringify({
          id: parsed.id ?? "WIS-CUSTOM-001",
          title: parsed.title ?? "Custom Rule",
          description: parsed.description ?? "",
          severity: parsed.severity ?? "WARN",
          enabled: true,
          hit_count: 0,
        }));
      });
      return;
    }

    if (req.method === "POST" && url === "/api/v1/wisdom/check") {
      let body = "";
      req.on("data", (chunk) => { body += chunk; });
      req.on("end", () => {
        const parsed = JSON.parse(body || "{}");
        const diff = (parsed.code_diff ?? "") as string;
        const hasBlock = diff.includes("UPDATE") || diff.includes("repository");
        res.end(JSON.stringify({
          hasBlockViolation: hasBlock,
          blockCount: hasBlock ? 1 : 0,
          warnCount: 0,
          matchedRules: hasBlock ? [{
            id: "WIS-DB-001",
            title: "UPDATE/DELETE 語句必須有 WHERE 條件",
            severity: "BLOCK",
          }] : [],
        }));
      });
      return;
    }

    // ── Experience cases ──────────────────────────────────────────────
    if (req.method === "POST" && url === "/api/v1/experience/search") {
      let body = "";
      req.on("data", (chunk) => { body += chunk; });
      req.on("end", () => {
        res.end(JSON.stringify([
          {
            id: "exp-001",
            project_id: "demo",
            title: "新增案件提醒功能",
            requirement: "系統需要在案件到期前 3 天發送提醒通知",
            solution_summary: "透過 Spring Scheduler 實作定時掃描",
            patterns_used: ["Scheduler", "Event"],
            _similarity: 0.87,
            reference_count: 5,
            outcome: "SUCCESS",
          },
          {
            id: "exp-002",
            project_id: "demo",
            title: "付款通知功能",
            requirement: "新增付款成功後的通知推送",
            solution_summary: "MQ 非同步推送",
            _similarity: 0.72,
            reference_count: 2,
            outcome: "SUCCESS",
          },
        ]));
      });
      return;
    }

    if (req.method === "GET" && url.startsWith("/api/v1/experience/cases")) {
      res.end(JSON.stringify([
        {
          id: "exp-001",
          project_id: "demo",
          title: "新增案件提醒功能",
          requirement: "系統需要在案件到期前 3 天發送提醒通知",
          outcome: "SUCCESS",
          reference_count: 5,
          created_at: "2026-06-01T10:00:00",
        },
      ]));
      return;
    }

    res.statusCode = 404;
    res.end("{}");
  });

  beforeAll(async () => {
    await new Promise<void>((resolveStart) => {
      server.listen(0, "127.0.0.1", () => {
        const address = server.address();
        if (address && typeof address !== "string") {
          serverPort = address.port;
        }
        resolveStart();
      });
    });
  });

  afterAll(async () => {
    await new Promise<void>((resolveStop) => server.close(() => resolveStop()));
  });

  // ── Wisdom tests ──────────────────────────────────────────────────
  it("wisdom list shows rules with severity icons", async () => {
    const stdout = await runCliWithRetry(["wisdom", "list"], serverPort);
    expect(stdout).toContain("BLOCK");
    expect(stdout).toContain("WIS-JAVA-001");
    expect(stdout).toContain("WIS-DB-001");
    expect(stdout).toContain("Total:");
  });

  it("wisdom add creates a new rule", async () => {
    const stdout = await runCliWithRetry([
      "wisdom", "add",
      "--title", "No hardcoded passwords",
      "--desc", "Never hardcode passwords in source",
      "--severity", "BLOCK",
    ], serverPort);
    expect(stdout).toContain("Wisdom rule created");
  });

  it("wisdom check detects BLOCK violation", async () => {
    const stdout = await runCliWithRetry([
      "wisdom", "check",
      "--diff", "UPDATE users SET password = 'test'",
    ], serverPort);
    expect(stdout).toContain("BLOCK violation");
    expect(stdout).toContain("WIS-DB-001");
  });

  it("wisdom check returns clean when no violation", async () => {
    const stdout = await runCliWithRetry([
      "wisdom", "check",
      "--diff", "void greet() { System.out.println(\"hello\"); }",
    ], serverPort);
    expect(stdout).toContain("No wisdom rule violations");
  });

  // ── Experience tests ──────────────────────────────────────────────
  it("experience search returns similar cases", async () => {
    const stdout = await runCliWithRetry([
      "experience", "search", "案件提醒功能",
      "--project", "demo",
    ], serverPort);
    expect(stdout).toContain("Found");
    expect(stdout).toContain("exp-001");
    expect(stdout).toContain("0.87");
  });

  it("experience list shows all cases", async () => {
    const stdout = await runCliWithRetry([
      "experience", "list",
      "--project", "demo",
    ], serverPort);
    expect(stdout).toContain("exp-001");
    expect(stdout).toContain("Total:");
  });
});

