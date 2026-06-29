#!/usr/bin/env node
/**
 * AIPA Studio CLI — 進入點（Phase 1 骨架）
 * 所有命令回傳「尚未實作」提示，Phase 2–4 逐步填充
 */
import { Command } from "commander";
import chalk from "chalk";

const program = new Command();

program
  .name("aipa")
  .description("AIPA Studio — AI Project Assistant Studio CLI")
  .version("1.0.0-SNAPSHOT");

// ── init ────────────────────────────────────────────────────────
program
  .command("init")
  .description("初始化專案，建立知識庫與 Project DNA")
  .action(() => {
    console.log(chalk.yellow("⚠️  aipa init — Phase 2 will implement Scanner & Knowledge Engine"));
    console.log(chalk.gray("   TODO: POST /api/v1/project/init"));
  });

// ── ask ─────────────────────────────────────────────────────────
program
  .command("ask <requirement>")
  .description("輸入需求，啟動完整 LSDD 開發週期")
  .action((requirement: string) => {
    console.log(chalk.yellow(`⚠️  aipa ask "${requirement}" — Phase 3 will implement Spec Engine`));
    console.log(chalk.gray("   TODO: POST /api/v1/session"));
  });

// ── scan ────────────────────────────────────────────────────────
program
  .command("scan")
  .description("重新掃描專案")
  .option("--target <path>", "指定掃描目標路徑")
  .action(() => {
    console.log(chalk.yellow("⚠️  aipa scan — Phase 2 will implement Scanner Engine"));
  });

// ── learn ───────────────────────────────────────────────────────
program
  .command("learn")
  .description("手動觸發學習（分析最新 PR）")
  .option("--pr <id>", "指定 PR ID")
  .action(() => {
    console.log(chalk.yellow("⚠️  aipa learn — Phase 5 will implement Learning Engine"));
  });

// ── checkpoint ──────────────────────────────────────────────────
const checkpoint = program.command("checkpoint").description("管理 Human Checkpoint");
checkpoint
  .command("list")
  .description("列出待審核的 Checkpoint")
  .action(() => console.log(chalk.yellow("⚠️  Phase 3 will implement Checkpoint Gate")));
checkpoint
  .command("approve <id>")
  .description("核准 Checkpoint")
  .action(() => console.log(chalk.yellow("⚠️  Phase 3 will implement Checkpoint Gate")));
checkpoint
  .command("reject <id>")
  .description("拒絕 Checkpoint")
  .action(() => console.log(chalk.yellow("⚠️  Phase 3 will implement Checkpoint Gate")));

// ── health ──────────────────────────────────────────────────────
program
  .command("health")
  .description("全系統健康檢查")
  .action(async () => {
    const { default: axios } = await import("axios");
    try {
      const res = await axios.get("http://localhost:18080/api/v1/health", { timeout: 3000 });
      console.log(chalk.green("✅ Runtime Service: UP"));
      console.log(chalk.gray(JSON.stringify(res.data, null, 2)));
    } catch {
      console.log(chalk.red("❌ Runtime Service: 無回應（請執行 aipa server start）"));
    }
  });

// ── version ─────────────────────────────────────────────────────
program
  .command("version")
  .description("顯示版本資訊")
  .action(() => {
    console.log(chalk.cyan("AIPA Studio CLI v1.0.0-SNAPSHOT (Phase 1 — Skeleton)"));
  });

// ── status ──────────────────────────────────────────────────────
program
  .command("status")
  .description("顯示目前 Session 狀態")
  .action(() => console.log(chalk.yellow("⚠️  Phase 3 will implement Session status")));

// ── server ──────────────────────────────────────────────────────
const server = program.command("server").description("管理 Runtime Service");
server.command("start").action(() => console.log(chalk.yellow("⚠️  Phase 1: use 'make docker-up' for now")));
server.command("stop").action(() => console.log(chalk.yellow("⚠️  Phase 1: use 'make docker-down' for now")));
server.command("status").action(async () => {
  const { default: axios } = await import("axios");
  try {
    const res = await axios.get("http://localhost:18080/api/v1/health", { timeout: 3000 });
    console.log(chalk.green(`✅ Runtime Service is UP — version ${res.data.version}`));
  } catch {
    console.log(chalk.red("❌ Runtime Service is DOWN"));
  }
});

program.parse();
