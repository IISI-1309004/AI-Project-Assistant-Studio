import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { createServer, IncomingMessage, ServerResponse } from "node:http";
import { spawn } from "node:child_process";

const rootDir = process.cwd();

async function runCli(args: string[], port: number, extraEnv: Record<string, string> = {}): Promise<{ stdout: string; stderr: string; code: number | null }> {
  return await new Promise((resolveOutput, rejectOutput) => {
    const child = spawn("node", ["./dist/index.js", ...args], {
      cwd: rootDir,
      env: {
        ...process.env,
        ...extraEnv,
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
      resolveOutput({ stdout, stderr, code });
    });
  });
}

describe("Phase 9 CLI doctor and context exclusion e2e", () => {
  let serverPort = 0;

  const server = createServer((req: IncomingMessage, res: ServerResponse) => {
    const url = req.url ?? "";
    res.setHeader("Content-Type", "application/json");

    if (req.method === "GET" && url === "/api/v1/health") {
      res.end(JSON.stringify({ status: "UP", version: "1.0.0-SNAPSHOT" }));
      return;
    }

    if (req.method === "POST" && url === "/api/v1/session") {
      let body = "";
      req.on("data", (chunk) => { body += chunk; });
      req.on("end", () => {
        const parsed = JSON.parse(body || "{}");
        res.end(JSON.stringify({
          sessionId: "s-phase9",
          projectId: "demo",
          status: "CHECKPOINT_PENDING",
          requirement: String(parsed.requirement ?? ""),
          specId: "spec-1",
          currentCheckpointId: "cp-1",
          confidenceScore: 80,
        }));
      });
      return;
    }

    if (req.method === "POST" && url === "/api/v1/wisdom/check") {
      res.end(JSON.stringify({
        hasBlockViolation: false,
        blockCount: 0,
        warnCount: 0,
        matchedRules: [],
      }));
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

  it("doctor --json returns structured diagnostics", async () => {
    const result = await runCli(["doctor", "--json"], serverPort, {
      AIPA_CONTEXT_EXCLUDE_PATTERNS: "secretValue\\\\d+",
      OPENAI_API_KEY: "sk-test-value",
    });

    expect([0, 1]).toContain(result.code ?? 1);
    expect(result.stdout).toContain("\"checks\"");
    expect(result.stdout).toContain("\"runtime\"");
    expect(result.stdout).toContain("\"context-exclude\"");
  });

  it("ask command redacts sensitive requirement before sending", async () => {
    const result = await runCli(["ask", "password=abc123 token=xyz"], serverPort);

    expect([0, 1]).toContain(result.code ?? 1);
    expect(result.stdout).toContain("已遮罩");
  });

  it("wisdom check redacts sensitive diff before sending", async () => {
    const result = await runCli([
      "wisdom",
      "check",
      "--diff",
      "const token=abc123; const password=mySecret;",
    ], serverPort);

    expect([0, 1]).toContain(result.code ?? 1);
    expect(result.stdout).toContain("已遮罩");
  });
});

