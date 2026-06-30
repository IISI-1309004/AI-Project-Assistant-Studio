const BUILTIN_SECRET_PATTERNS: RegExp[] = [
  /(?:api[_-]?key|token|password|secret)\s*[:=]\s*[^\s,;]+/gi,
  /sk-[a-z0-9]{16,}/gi,
  /ghp_[a-z0-9]{16,}/gi,
];

export function parseExcludePatterns(raw: string | undefined): RegExp[] {
  if (!raw?.trim()) {
    return [];
  }

  return raw
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .flatMap((item) => {
      try {
        return [new RegExp(item, "gi")];
      } catch {
        return [];
      }
    });
}

export function redactSensitiveText(input: string, customPatternsRaw = process.env.AIPA_CONTEXT_EXCLUDE_PATTERNS): { sanitized: string; redactedCount: number } {
  const customPatterns = parseExcludePatterns(customPatternsRaw);
  const patterns = [...BUILTIN_SECRET_PATTERNS, ...customPatterns];

  let redactedCount = 0;
  let sanitized = input;
  for (const pattern of patterns) {
    sanitized = sanitized.replace(pattern, (match) => {
      redactedCount += 1;
      const keyOnly = match.includes("=")
        ? match.split("=")[0]
        : match.includes(":")
          ? match.split(":")[0]
          : "secret";
      return `${keyOnly}=[REDACTED]`;
    });
  }

  return { sanitized, redactedCount };
}

