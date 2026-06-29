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
    child.stdout.on("data", (chunk) => {
      stdout += chunk.toString();
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk.toString();
    });
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

describe("Phase 5 CLI learning and memory e2e", () => {
  let serverPort = 0;
  const server = createServer((req: IncomingMessage, res: ServerResponse) => {
    const url = req.url ?? "";

    if (req.method === "POST" && url === "/api/v1/learn") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({
        learning_id: "learn-1",
        new_knowledge_count: 2,
        new_memory_count: 2,
      }));
      return;
    }

    if (req.method === "GET" && url === "/api/v1/learn/learn-1") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({
        learning_id: "learn-1",
        status: "COMPLETED",
        summary: "merge feature",
      }));
      return;
    }

    if (req.method === "POST" && url === "/api/v1/learn/learn-1/rollback") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({
        learning_id: "learn-1",
        message: "Learning rollback completed",
      }));
      return;
    }

    // Phase 5-2: Auto-learning support
    if (req.method === "GET" && url === "/api/v1/session") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify([{
        sessionId: "s-auto-test",
        projectId: "demo",
        status: "COMPLETED",
        requirement: "Auto-learning test feature",
        specTitle: "Auto-learning feature spec",
        spec: { title: "Auto-learning feature spec" },
        execution: { status: "PR_READY", ai: { provider: "openai" } },
      }]));
      return;
    }

    if (req.method === "GET" && url === "/api/v1/session/s-auto-test/memory-reinforcement") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({
        sessionId: "s-auto-test",
        status: "AVAILABLE",
        memoryReinforcement: {
          enabled: true,
          attempted: 2,
          reinforced: 2,
          failed: 0,
        },
      }));
      return;
    }

    if (req.method === "GET" && url.startsWith("/api/v1/memory")) {
      res.setHeader("Content-Type", "application/json");
      if (url === "/api/v1/memory/m-1") {
        res.end(JSON.stringify({ id: "m-1", type: "PATTERN", key: "layered", strength: 7 }));
      } else {
        res.end(JSON.stringify([{ id: "m-1", type: "PATTERN", key: "layered", strength: 6 }]));
      }
      return;
    }

    if (req.method === "POST" && url === "/api/v1/memory/m-1/reinforce") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({ id: "m-1", type: "PATTERN", key: "layered", strength: 7 }));
      return;
    }

    res.statusCode = 404;
    res.end();
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

  it("supports learn command", async () => {
    const stdout = await runCli(["learn", "--project-id", "demo", "--pr", "42", "--summary", "merge feature"], serverPort);
    expect(stdout).toContain("Learning done: learn-1");
  });

  it("supports learn-result and rollback", async () => {
    const result = await runCli(["learn-result", "learn-1"], serverPort);
    expect(result).toContain("\"status\": \"COMPLETED\"");

    const rollback = await runCli(["learn-rollback", "learn-1"], serverPort);
    expect(rollback).toContain("Learning rollback completed");
  });

  it("supports memory list/show/reinforce", async () => {
    const list = await runCli(["memory", "list", "--project-id", "demo"], serverPort);
    expect(list).toContain("m-1");

    const show = await runCli(["memory", "show", "m-1"], serverPort);
    expect(show).toContain("\"strength\": 7");

    const reinforce = await runCli(["memory", "reinforce", "m-1"], serverPort);
    expect(reinforce).toContain("\"strength\": 7");
  });

  /**
   * Phase 5-2: Auto-learning tests
   */
  it("supports learn --auto from completed session", async () => {
    const stdout = await runCli(["learn", "--auto"], serverPort);
    expect(stdout).toContain("Auto-learning from session");
    expect(stdout).toContain("Auto-Learning triggered");
  });

  it("supports learn-progress to check learning status", async () => {
    const stdout = await runCli(["learn-progress", "learn-1"], serverPort);
    expect(stdout).toContain("Status:");
    expect(stdout).toContain("COMPLETED");
  });

  it("supports session-memory command", async () => {
    const stdout = await runCli(["session-memory", "s-auto-test"], serverPort);
    expect(stdout).toContain("\"status\": \"AVAILABLE\"");
    expect(stdout).toContain("\"reinforced\": 2");
  });
});

