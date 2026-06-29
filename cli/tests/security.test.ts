import { describe, expect, it } from "vitest";
import { parseExcludePatterns, redactSensitiveText } from "../src/security";

describe("security redaction", () => {
  it("redacts built-in secret key patterns", () => {
    const input = "password=abc123 token=xyz";
    const result = redactSensitiveText(input, "");

    expect(result.redactedCount).toBeGreaterThan(0);
    expect(result.sanitized).toContain("password=[REDACTED]");
    expect(result.sanitized).toContain("token=[REDACTED]");
    expect(result.sanitized).not.toContain("abc123");
    expect(result.sanitized).not.toContain("xyz");
  });

  it("supports custom exclude patterns", () => {
    const input = "my custom marker secretValue123 should be hidden";
    const result = redactSensitiveText(input, "secretValue\\d+");

    expect(result.redactedCount).toBe(1);
    expect(result.sanitized).toContain("secret=[REDACTED]");
    expect(result.sanitized).not.toContain("secretValue123");
  });

  it("ignores invalid custom regex safely", () => {
    const patterns = parseExcludePatterns("[invalid-regex");
    expect(patterns.length).toBe(0);
  });
});

