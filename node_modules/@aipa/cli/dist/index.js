#!/usr/bin/env node
"use strict";
var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));

// src/index.ts
var import_commander = require("commander");
var import_chalk = __toESM(require("chalk"));
var import_axios = __toESM(require("axios"));
var import_node_path = require("path");
var RUNTIME_URL = process.env.AIPA_RUNTIME_URL ?? "http://localhost:18080";
var http = import_axios.default.create({
  baseURL: RUNTIME_URL,
  timeout: 1e4
});
var sleep = (ms) => new Promise((resolveTimer) => setTimeout(resolveTimer, ms));
function normalizeProjectId(input) {
  return input.toLowerCase().replace(/[^a-z0-9-_]+/g, "-").replace(/^-+|-+$/g, "") || "default";
}
async function waitForInit(jobId) {
  let latest = {
    jobId,
    status: "STARTED",
    progress: 0,
    message: "Waiting for initialization"
  };
  while (true) {
    const { data } = await http.get(`/api/v1/project/init/${jobId}/status`);
    latest = data;
    process.stdout.write(`\r${import_chalk.default.cyan("[init]")} ${latest.progress}% ${latest.message.padEnd(48)}`);
    if (latest.status === "COMPLETED" || latest.status === "FAILED") {
      process.stdout.write("\n");
      return latest;
    }
    await sleep(1200);
  }
}
var program = new import_commander.Command();
program.name("aipa").description("AIPA Studio \u2014 AI Project Assistant Studio CLI").version("1.0.0-SNAPSHOT");
program.command("init").description("\u521D\u59CB\u5316\u5C08\u6848\uFF0C\u5EFA\u7ACB\u77E5\u8B58\u5EAB\u8207 Project DNA").option("--project-root <path>", "\u6383\u63CF\u76EE\u9304", process.cwd()).option("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC").option("--no-wait", "\u53EA\u9001\u51FA job\uFF0C\u4E0D\u7B49\u5F85\u5B8C\u6210").action(async (opts) => {
  const projectRoot = (0, import_node_path.resolve)(opts.projectRoot);
  const projectId = opts.projectId ?? normalizeProjectId((0, import_node_path.basename)(projectRoot));
  const { data } = await http.post("/api/v1/project/init", {
    projectRoot,
    projectId
  });
  console.log(import_chalk.default.green(`Init job started: ${data.jobId}`));
  console.log(import_chalk.default.gray(`Runtime: ${RUNTIME_URL}`));
  console.log(import_chalk.default.gray(`Project: ${projectId} @ ${projectRoot}`));
  if (!opts.wait) {
    console.log(import_chalk.default.yellow(`Use: aipa init-status ${data.jobId}`));
    return;
  }
  const final = await waitForInit(data.jobId);
  if (final.status === "COMPLETED") {
    console.log(import_chalk.default.green("Project init completed."));
    if (final.summary) {
      console.log(import_chalk.default.gray(JSON.stringify(final.summary, null, 2)));
    }
    return;
  }
  console.error(import_chalk.default.red(`Project init failed: ${final.message}`));
  process.exitCode = 1;
});
program.command("init-status <jobId>").description("\u67E5\u8A62 init job \u72C0\u614B").action(async (jobId) => {
  const { data } = await http.get(`/api/v1/project/init/${jobId}/status`);
  console.log(JSON.stringify(data, null, 2));
});
program.command("ask <requirement>").description("\u7522\u751F\u898F\u683C\u3001\u4FE1\u5FC3\u8A55\u4F30\u8207 Task Planning\uFF08Phase 3 MVP\uFF09").option("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC", "default").option("--project-root <path>", "\u5C08\u6848\u8DEF\u5F91", process.cwd()).action(async (requirement, opts) => {
  const { data } = await http.post("/api/v1/session", {
    requirement,
    projectId: opts.projectId,
    projectRoot: (0, import_node_path.resolve)(opts.projectRoot)
  });
  console.log(import_chalk.default.green(`Session created: ${data.sessionId}`));
  console.log(import_chalk.default.cyan(`Status: ${data.status}`));
  console.log(import_chalk.default.gray(`Spec: ${data.specId ?? "N/A"}`));
  console.log(import_chalk.default.gray(`Checkpoint: ${data.currentCheckpointId ?? "N/A"}`));
  console.log(import_chalk.default.gray(`Confidence: ${data.confidenceScore ?? 0}`));
  if (data.spec?.title) {
    console.log(import_chalk.default.white(`Spec Title: ${data.spec.title}`));
  }
  if (data.message) {
    console.log(import_chalk.default.yellow(data.message));
  }
  console.log(import_chalk.default.gray(`Next: aipa checkpoint list --session-id ${data.sessionId}`));
});
program.command("scan").description("\u91CD\u65B0\u6383\u63CF\u5C08\u6848").option("--target <path>", "\u6307\u5B9A\u6383\u63CF\u76EE\u6A19\u8DEF\u5F91", process.cwd()).option("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC").action(async (opts) => {
  const target = (0, import_node_path.resolve)(opts.target);
  const projectId = opts.projectId ?? normalizeProjectId((0, import_node_path.basename)(target));
  const { data } = await http.post("/api/v1/project/init", {
    projectRoot: target,
    projectId,
    fullRescan: true
  });
  console.log(import_chalk.default.green(`Rescan job started: ${data.jobId}`));
  const final = await waitForInit(data.jobId);
  if (final.status !== "COMPLETED") {
    process.exitCode = 1;
  }
});
program.command("learn").description("\u624B\u52D5\u89F8\u767C\u5B78\u7FD2\uFF08\u5206\u6790\u6700\u65B0 PR\uFF09").option("--pr <id>", "\u6307\u5B9A PR ID").option("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC", "default").option("--session-id <id>", "\u4F86\u6E90 Session ID", "").option("--summary <text>", "\u5B78\u7FD2\u6458\u8981", "Merged PR learning").option("--files <items>", "\u8B8A\u66F4\u6A94\u6848\uFF08\u9017\u865F\u5206\u9694\uFF09", "").option("--review-comments <items>", "\u5BE9\u67E5\u5EFA\u8B70\uFF08\u9017\u865F\u5206\u9694\uFF09", "").option("--auto", "\u81EA\u52D5\u5F9E\u6700\u8FD1\u5B8C\u6210\u7684 Session \u89F8\u767C\u5B78\u7FD2").action(async (opts) => {
  if (opts.auto) {
    try {
      const sessions = await http.get("/api/v1/session");
      if (!Array.isArray(sessions.data) || sessions.data.length === 0) {
        console.log(import_chalk.default.yellow("No sessions found for auto-learning trigger."));
        return;
      }
      const completedSession = sessions.data.find((s) => s.status === "COMPLETED");
      if (!completedSession) {
        console.log(import_chalk.default.yellow("No completed sessions found. Use 'aipa learn' with manual options."));
        return;
      }
      console.log(import_chalk.default.cyan(`Auto-learning from session: ${completedSession.sessionId}`));
      const changedFiles2 = completedSession.execution?.ai?.generatedFiles ?? [];
      const learningData = {
        project_id: completedSession.projectId,
        pr_id: "auto-" + completedSession.sessionId,
        session_id: completedSession.sessionId,
        summary: `Completed: ${completedSession.spec?.title ?? "Feature"}`,
        changed_files: changedFiles2,
        review_comments: ["AI-generated implementation", `Status: ${completedSession.status}`]
      };
      const { data: data2 } = await http.post("/api/v1/learn", learningData);
      console.log(import_chalk.default.green(`Auto-Learning triggered: ${data2.learning_id}`));
      console.log(import_chalk.default.gray(`+knowledge: ${data2.new_knowledge_count}, +memory: ${data2.new_memory_count}`));
      console.log(import_chalk.default.gray(`Use: aipa learn-result ${data2.learning_id}`));
    } catch (err) {
      console.error(import_chalk.default.red(`Auto-learning failed: ${err instanceof Error ? err.message : String(err)}`));
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
    review_comments: reviewComments
  });
  console.log(import_chalk.default.green(`Learning done: ${data.learning_id}`));
  console.log(import_chalk.default.gray(`+knowledge: ${data.new_knowledge_count}, +memory: ${data.new_memory_count}`));
  console.log(import_chalk.default.gray(`Use: aipa learn-result ${data.learning_id}`));
});
program.command("learn-result <learningId>").description("\u67E5\u8A62\u5B78\u7FD2\u7D50\u679C").action(async (learningId) => {
  const { data } = await http.get(`/api/v1/learn/${learningId}`);
  console.log(JSON.stringify(data, null, 2));
});
program.command("learn-progress <learningId>").description("\u67E5\u8A62\u5B78\u7FD2\u9032\u5EA6").action(async (learningId) => {
  try {
    const { data } = await http.get(`/api/v1/learn/${learningId}`);
    const status = data.status ?? "UNKNOWN";
    const summary = data.summary ?? "";
    const progress = data.progress ?? 0;
    const statusColor = status === "COMPLETED" ? import_chalk.default.green : status === "FAILED" ? import_chalk.default.red : status === "IN_PROGRESS" ? import_chalk.default.cyan : import_chalk.default.yellow;
    console.log(import_chalk.default.cyan(`Learning Progress [${learningId}]`));
    console.log(`Status: ${statusColor(status)}`);
    console.log(`Progress: ${progress}%`);
    if (summary) {
      console.log(`Summary: ${summary}`);
    }
    if (data.new_knowledge_count) {
      console.log(import_chalk.default.green(`\u2713 New Knowledge: ${data.new_knowledge_count}`));
    }
    if (data.new_memory_count) {
      console.log(import_chalk.default.green(`\u2713 New Memory: ${data.new_memory_count}`));
    }
    if (status === "FAILED" && data.error) {
      console.log(import_chalk.default.red(`Error: ${data.error}`));
    }
  } catch (err) {
    console.error(import_chalk.default.red(`Failed to get learning progress: ${err instanceof Error ? err.message : String(err)}`));
    process.exitCode = 1;
  }
});
program.command("learn-rollback <learningId>").description("\u56DE\u6EFE\u5B78\u7FD2\u7D50\u679C").action(async (learningId) => {
  const { data } = await http.post(`/api/v1/learn/${learningId}/rollback`, {});
  console.log(JSON.stringify(data, null, 2));
});
var checkpoint = program.command("checkpoint").description("\u7BA1\u7406 Human Checkpoint");
checkpoint.command("list").description("\u5217\u51FA\u5F85\u5BE9\u6838\u7684 Checkpoint").option("--session-id <id>", "\u53EA\u5217\u51FA\u7279\u5B9A Session \u7684 Checkpoint").action(async (opts) => {
  const { data } = await http.get("/api/v1/checkpoint", {
    params: { sessionId: opts.sessionId }
  });
  if (!Array.isArray(data) || data.length === 0) {
    console.log(import_chalk.default.yellow("No pending checkpoints."));
    return;
  }
  for (const item of data) {
    console.log(`- ${item.checkpointId} [${item.type}] session=${item.sessionId} status=${item.status}`);
  }
});
checkpoint.command("approve <id>").description("\u6838\u51C6 Checkpoint").option("--comments <text>", "\u5BE9\u6838\u5099\u8A3B", "").action(async (id, opts) => {
  const { data } = await http.post(`/api/v1/checkpoint/${id}/approve`, {
    actor: "cli",
    comments: opts.comments
  });
  const session = data.session;
  console.log(import_chalk.default.green(`Checkpoint approved: ${id}`));
  console.log(import_chalk.default.cyan(`Session ${session.sessionId} => ${session.status}`));
  if (session.nmiReport) {
    console.log(import_chalk.default.yellow(session.nmiReport));
  }
  if (session.taskPlan?.tasks && session.taskPlan.tasks.length > 0) {
    console.log(import_chalk.default.green(`Task Plan ${session.taskPlan.planId ?? ""}`));
    for (const task of session.taskPlan.tasks) {
      console.log(`- ${task.id} ${task.title}`);
    }
  }
  if (session.phase4Message) {
    console.log(import_chalk.default.yellow(session.phase4Message));
  }
  if (session.execution) {
    console.log(import_chalk.default.green(`Execution: ${session.execution.status ?? "UNKNOWN"}`));
    if (session.execution.ai?.provider) {
      console.log(import_chalk.default.gray(`AI Provider: ${session.execution.ai.provider} (${session.execution.ai.model ?? "model-unknown"})`));
    }
    if (session.execution.prPreview?.title) {
      console.log(import_chalk.default.gray(`PR Preview: ${session.execution.prPreview.title} @ ${session.execution.prPreview.branch ?? "n/a"}`));
    }
  }
  if (session.memoryReinforcement?.enabled) {
    console.log(import_chalk.default.green(`Memory Reinforcement: ${session.memoryReinforcement.reinforced ?? 0}/${session.memoryReinforcement.attempted ?? 0}`));
    if ((session.memoryReinforcement.failed ?? 0) > 0) {
      console.log(import_chalk.default.yellow(`Memory reinforce failed count: ${session.memoryReinforcement.failed}`));
    }
  }
});
checkpoint.command("reject <id>").description("\u62D2\u7D55 Checkpoint").option("--comments <text>", "\u62D2\u7D55\u539F\u56E0", "").action(async (id, opts) => {
  const { data } = await http.post(`/api/v1/checkpoint/${id}/reject`, {
    actor: "cli",
    comments: opts.comments
  });
  console.log(import_chalk.default.red(`Checkpoint rejected: ${id}`));
  console.log(import_chalk.default.cyan(`Session ${data.session.sessionId} => ${data.session.status}`));
});
program.command("health").description("\u5168\u7CFB\u7D71\u5065\u5EB7\u6AA2\u67E5").action(async () => {
  try {
    const res = await http.get("/api/v1/health", { timeout: 3e3 });
    console.log(import_chalk.default.green("\u2705 Runtime Service: UP"));
    console.log(import_chalk.default.gray(JSON.stringify(res.data, null, 2)));
  } catch {
    console.log(import_chalk.default.red("\u274C Runtime Service: \u7121\u56DE\u61C9\uFF08\u8ACB\u57F7\u884C aipa server start\uFF09"));
  }
});
var knowledge = program.command("knowledge").description("\u77E5\u8B58\u5EAB\u67E5\u8A62");
var memory = program.command("memory").description("\u8A18\u61B6\u67E5\u8A62");
knowledge.command("search <query>").description("\u641C\u5C0B\u77E5\u8B58\u5EAB").option("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC", "default").option("--top-k <n>", "\u56DE\u50B3\u7B46\u6578", "5").action(async (query, opts) => {
  const { data } = await http.post("/api/v1/knowledge/search", {
    query,
    projectId: opts.projectId,
    topK: Number(opts.topK)
  });
  if (!Array.isArray(data) || data.length === 0) {
    console.log(import_chalk.default.yellow("No knowledge found."));
    return;
  }
  for (const item of data) {
    console.log(`- ${item.title} [${item.category}] (score=${(item._score ?? 0).toFixed?.(3) ?? item._score ?? 0})`);
  }
});
memory.command("list").description("\u5217\u51FA\u8A18\u61B6\u689D\u76EE").requiredOption("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC").option("--type <name>", "\u8A18\u61B6\u985E\u578B").action(async (opts) => {
  const { data } = await http.get("/api/v1/memory", {
    params: {
      projectId: opts.projectId,
      type: opts.type
    }
  });
  if (!Array.isArray(data) || data.length === 0) {
    console.log(import_chalk.default.yellow("Memory is empty."));
    return;
  }
  for (const item of data) {
    console.log(`- ${item.id} ${item.type} ${item.key} (strength=${item.strength})`);
  }
});
memory.command("show <id>").description("\u986F\u793A\u55AE\u4E00\u8A18\u61B6").action(async (id) => {
  const { data } = await http.get(`/api/v1/memory/${id}`);
  console.log(JSON.stringify(data, null, 2));
});
memory.command("reinforce <id>").description("\u5F37\u5316\u8A18\u61B6").action(async (id) => {
  const { data } = await http.post(`/api/v1/memory/${id}/reinforce`, {});
  console.log(JSON.stringify(data, null, 2));
});
knowledge.command("list").description("\u5217\u51FA\u77E5\u8B58\u9805\u76EE").requiredOption("--project-id <id>", "\u5C08\u6848\u8B58\u5225\u78BC").option("--category <name>", "\u5206\u985E\u904E\u6FFE").action(async (opts) => {
  const { data } = await http.get("/api/v1/knowledge", {
    params: {
      projectId: opts.projectId,
      category: opts.category
    }
  });
  if (!Array.isArray(data) || data.length === 0) {
    console.log(import_chalk.default.yellow("Knowledge base is empty."));
    return;
  }
  for (const item of data) {
    console.log(`- ${item.id} ${item.title} [${item.category}]`);
  }
});
program.command("version").description("\u986F\u793A\u7248\u672C\u8CC7\u8A0A").action(() => {
  console.log(import_chalk.default.cyan("AIPA Studio CLI v1.0.0-SNAPSHOT (Phase 5-2 \u2014 Auto-Learning)"));
});
program.command("status").description("\u986F\u793A\u76EE\u524D Session \u72C0\u614B").option("--memory", "\u53EA\u986F\u793A Session \u7684 memory reinforcement \u72C0\u614B").argument("[sessionId]", "Session ID").action(async (sessionId, opts) => {
  if (sessionId && opts.memory) {
    const { data: data2 } = await http.get(`/api/v1/session/${sessionId}/memory-reinforcement`);
    console.log(JSON.stringify(data2, null, 2));
    return;
  }
  if (sessionId) {
    const { data: data2 } = await http.get(`/api/v1/session/${sessionId}`);
    console.log(JSON.stringify(data2, null, 2));
    return;
  }
  const { data } = await http.get("/api/v1/session");
  if (!Array.isArray(data) || data.length === 0) {
    console.log(import_chalk.default.yellow("No sessions found."));
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
program.command("session-memory <sessionId>").description("\u67E5\u8A62\u6307\u5B9A Session \u7684 memory reinforcement \u7D50\u679C").action(async (sessionId) => {
  try {
    const { data } = await http.get(`/api/v1/session/${sessionId}/memory-reinforcement`);
    console.log(JSON.stringify(data, null, 2));
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to query memory reinforcement: ${message}`));
    process.exitCode = 1;
  }
});
program.command("session-summary <sessionId>").description("\u67E5\u8A62\u6307\u5B9A Session \u7684 completion summary\uFF08learning + memory\uFF09").action(async (sessionId) => {
  try {
    const { data } = await http.get(`/api/v1/session/${sessionId}/summary`);
    console.log(JSON.stringify(data, null, 2));
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to query session summary: ${message}`));
    process.exitCode = 1;
  }
});
var server = program.command("server").description("\u7BA1\u7406 Runtime Service");
server.command("start").action(() => console.log(import_chalk.default.yellow("\u26A0\uFE0F  Phase 1: use 'make docker-up' for now")));
server.command("stop").action(() => console.log(import_chalk.default.yellow("\u26A0\uFE0F  Phase 1: use 'make docker-down' for now")));
server.command("status").action(async () => {
  try {
    const res = await http.get("/api/v1/health", { timeout: 3e3 });
    console.log(import_chalk.default.green(`\u2705 Runtime Service is UP \u2014 version ${res.data.version}`));
  } catch {
    console.log(import_chalk.default.red("\u274C Runtime Service is DOWN"));
  }
});
var wisdom = program.command("wisdom").description("\u667A\u6167\u898F\u5247\u7BA1\u7406\uFF08Phase 6\uFF09");
wisdom.command("list").description("\u5217\u51FA\u6240\u6709\u667A\u6167\u898F\u5247").option("--project <projectId>", "\u7BE9\u9078\u7279\u5B9A\u5C08\u6848\u7684\u898F\u5247", "").action(async (opts) => {
  try {
    const url = opts.project ? `/api/v1/wisdom/rules?projectId=${opts.project}` : "/api/v1/wisdom/rules";
    const { data } = await http.get(url);
    if (!data || data.length === 0) {
      console.log(import_chalk.default.yellow("No wisdom rules found."));
      return;
    }
    data.forEach((rule) => {
      const severity = rule["severity"];
      const icon = severity === "BLOCK" ? import_chalk.default.red("\u{1F6AB} BLOCK") : import_chalk.default.yellow("\u26A0\uFE0F  WARN ");
      console.log(`${icon}  [${rule["id"]}] ${rule["title"]}`);
      if (rule["description"]) {
        console.log(`        ${import_chalk.default.gray(String(rule["description"]).substring(0, 80))}`);
      }
    });
    console.log(import_chalk.default.gray(`
Total: ${data.length} rules`));
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to list wisdom rules: ${message}`));
    process.exitCode = 1;
  }
});
wisdom.command("add").description("\u65B0\u589E\u667A\u6167\u898F\u5247").requiredOption("--title <title>", "\u898F\u5247\u6A19\u984C").requiredOption("--desc <description>", "\u898F\u5247\u63CF\u8FF0").option("--severity <severity>", "\u56B4\u91CD\u7B49\u7D1A WARN|BLOCK", "WARN").option("--id <id>", "\u898F\u5247 ID\uFF08\u9078\u586B\uFF09").option("--condition <condition>", "\u89F8\u767C\u689D\u4EF6").action(async (opts) => {
  try {
    const rule = {
      title: opts.title,
      description: opts.desc,
      severity: opts.severity.toUpperCase(),
      scope: { global: true },
      trigger_conditions: opts.condition ? [opts.condition] : [],
      enabled: true
    };
    if (opts.id) rule["id"] = opts.id;
    const { data } = await http.post("/api/v1/wisdom/rules", rule);
    console.log(import_chalk.default.green(`\u2705 Wisdom rule created: ${data["id"]}`));
    console.log(`   Title: ${data["title"]}`);
    console.log(`   Severity: ${data["severity"]}`);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to add wisdom rule: ${message}`));
    process.exitCode = 1;
  }
});
wisdom.command("check").description("\u5C0D\u7A0B\u5F0F\u78BC\u8B8A\u66F4\u57F7\u884C\u667A\u6167\u898F\u5247\u6AA2\u67E5").option("--diff <codeDiff>", "\u7A0B\u5F0F\u78BC diff \u5167\u5BB9", "").option("--files <files>", "\u7570\u52D5\u6A94\u6848\uFF08\u9017\u865F\u5206\u9694\uFF09", "").option("--type <specType>", "Spec \u985E\u578B", "FEATURE").action(async (opts) => {
  try {
    const context = {
      code_diff: opts.diff,
      file_names: opts.files ? opts.files.split(",").map((f) => f.trim()) : [],
      spec_type: opts.type
    };
    const { data } = await http.post("/api/v1/wisdom/check", context);
    if (data.hasBlockViolation) {
      console.log(import_chalk.default.red(`\u{1F6AB} BLOCK violation! (${data.blockCount} BLOCK, ${data.warnCount} WARN)`));
    } else if (data.warnCount > 0) {
      console.log(import_chalk.default.yellow(`\u26A0\uFE0F  ${data.warnCount} WARN rule(s) matched.`));
    } else {
      console.log(import_chalk.default.green("\u2705 No wisdom rule violations."));
    }
    (data.matchedRules ?? []).forEach((rule) => {
      const icon = rule["severity"] === "BLOCK" ? import_chalk.default.red("\u{1F6AB}") : import_chalk.default.yellow("\u26A0\uFE0F");
      console.log(`  ${icon} [${rule["id"]}] ${rule["title"]}`);
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to check wisdom rules: ${message}`));
    process.exitCode = 1;
  }
});
var experience = program.command("experience").description("\u7D93\u9A57\u6848\u4F8B\u7BA1\u7406\uFF08Phase 6\uFF09");
experience.command("search <query>").description("\u641C\u5C0B\u76F8\u4F3C\u6B77\u53F2\u6848\u4F8B\uFF08\u76F8\u4F3C\u5EA6 > 0.6\uFF09").option("--project <projectId>", "\u9650\u5B9A\u5C08\u6848 ID", "").option("--top <n>", "\u56DE\u50B3\u7B46\u6578", "5").action(async (query, opts) => {
  try {
    const { data } = await http.post("/api/v1/experience/search", {
      query,
      project_id: opts.project,
      top_k: parseInt(opts.top, 10)
    });
    if (!data || data.length === 0) {
      console.log(import_chalk.default.yellow("No similar cases found (similarity threshold: 0.6)."));
      return;
    }
    console.log(import_chalk.default.green(`Found ${data.length} similar case(s):
`));
    data.forEach((c, i) => {
      const sim = typeof c["_similarity"] === "number" ? ` (similarity: ${c["_similarity"].toFixed(2)})` : "";
      console.log(`${i + 1}. [${c["id"]}] ${c["title"]}${sim}`);
      if (c["requirement"]) {
        console.log(`   ${import_chalk.default.gray(String(c["requirement"]).substring(0, 100))}`);
      }
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to search experience cases: ${message}`));
    process.exitCode = 1;
  }
});
experience.command("list").description("\u5217\u51FA\u6240\u6709\u6B77\u53F2\u6848\u4F8B").requiredOption("--project <projectId>", "\u5C08\u6848 ID").action(async (opts) => {
  try {
    const { data } = await http.get(
      `/api/v1/experience/cases?project_id=${opts.project}`
    );
    if (!data || data.length === 0) {
      console.log(import_chalk.default.yellow("No experience cases found."));
      return;
    }
    console.log(import_chalk.default.bold(`Experience Cases for '${opts.project}':
`));
    data.forEach((c, i) => {
      const icon = c["outcome"] === "SUCCESS" ? import_chalk.default.green("\u2705") : import_chalk.default.yellow("\u26A0\uFE0F");
      console.log(`${i + 1}. ${icon} [${c["id"]}] ${c["title"]}`);
      console.log(`   Created: ${c["created_at"]}  References: ${c["reference_count"] ?? 0}`);
    });
    console.log(import_chalk.default.gray(`
Total: ${data.length} cases`));
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(import_chalk.default.red(`Failed to list experience cases: ${message}`));
    process.exitCode = 1;
  }
});
program.parseAsync().catch((err) => {
  const message = err instanceof Error ? err.message : String(err);
  console.error(import_chalk.default.red(`Command failed: ${message}`));
  process.exitCode = 1;
});
