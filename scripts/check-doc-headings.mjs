#!/usr/bin/env node
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const ROOT = process.cwd();
const DOCS_TO_CHECK = [
  "docs/user-guide.md",
  "docs/installation-guide.md",
];

function slugifyHeading(text) {
  return text
    .trim()
    .toLowerCase()
    .replace(/[\u2013\u2014]/g, "-") // en/em dash
    .replace(/`/g, "")
    .replace(/[!"#$%&'()*+,./:;<=>?@[\\\]^_{|}~]/g, "")
    .replace(/[，。！？：；、（）【】《》〈〉「」『』]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function parseHeadings(markdown) {
  const headings = [];
  const lines = markdown.split(/\r?\n/);
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    const match = line.match(/^(#{2,6})\s+(.+)$/);
    if (!match) {
      continue;
    }

    const title = match[2].trim();
    // Ignore code fence headings and separators
    if (title.startsWith("```")) {
      continue;
    }

    headings.push({
      line: i + 1,
      level: match[1].length,
      title,
      anchor: slugifyHeading(title),
    });
  }
  return headings;
}

function parseTocLinks(markdown) {
  const lines = markdown.split(/\r?\n/);
  const tocStart = lines.findIndex((line) => line.trim() === "## 目錄");
  if (tocStart < 0) {
    return [];
  }

  let tocEnd = lines.findIndex((line, idx) => idx > tocStart && /^##\s+/.test(line));
  if (tocEnd < 0) {
    tocEnd = lines.length;
  }

  const links = [];
  for (let i = tocStart; i < tocEnd; i += 1) {
    const line = lines[i];
    const regex = /\[[^\]]+\]\((#[^)]+)\)/g;
    let m = regex.exec(line);
    while (m) {
      links.push({
        line: i + 1,
        raw: m[1],
        normalized: slugifyHeading(m[1].replace(/^#/, "")),
      });
      m = regex.exec(line);
    }
  }
  return links;
}

async function checkFile(relativePath) {
  const filePath = resolve(ROOT, relativePath);
  const markdown = await readFile(filePath, "utf-8");

  const headings = parseHeadings(markdown);
  const anchors = new Set(headings.map((h) => h.anchor));
  const tocLinks = parseTocLinks(markdown);

  const missing = tocLinks.filter((link) => !anchors.has(link.normalized));

  return {
    relativePath,
    headingCount: headings.length,
    tocLinkCount: tocLinks.length,
    missing,
  };
}

async function main() {
  const results = await Promise.all(DOCS_TO_CHECK.map(checkFile));

  let hasError = false;
  for (const result of results) {
    console.log(`\n[check] ${result.relativePath}`);
    console.log(`  headings: ${result.headingCount}`);
    console.log(`  toc links: ${result.tocLinkCount}`);

    if (result.missing.length === 0) {
      console.log("  status: OK");
      continue;
    }

    hasError = true;
    console.log("  status: FAIL");
    for (const item of result.missing) {
      console.log(`  - line ${item.line}: missing anchor ${item.raw}`);
    }
  }

  if (hasError) {
    process.exitCode = 1;
  }
}

main().catch((err) => {
  console.error(`[error] ${err instanceof Error ? err.message : String(err)}`);
  process.exitCode = 1;
});

