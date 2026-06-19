import { describe, expect, it } from "vitest";
import { cn, formatMoney } from "@/lib/utils";

/**
 * Unit tests for the pure utility helpers. These have no React/network deps so they
 * run fast and document the expected behavior of money formatting and class merging.
 */
describe("cn", () => {
  it("merges conflicting tailwind classes so the last one wins", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
  });

  it("drops falsy values and keeps conditional classes", () => {
    expect(cn("a", false && "b", undefined, "c")).toBe("a c");
  });
});

describe("formatMoney", () => {
  it("formats a string amount as USD in en locale", () => {
    // Non-breaking spaces/grouping vary by ICU build, so assert on the substring.
    const out = formatMoney("1234.5", "USD", "en");
    expect(out).toContain("1,234.5");
    expect(out).toContain("$");
  });

  it("falls back gracefully for an unknown currency code", () => {
    const out = formatMoney("10", "ZZZ", "en");
    expect(out).toContain("10");
  });
});
