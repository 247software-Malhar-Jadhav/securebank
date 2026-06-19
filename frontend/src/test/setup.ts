import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

/**
 * Vitest global setup. Imports jest-dom matchers (toBeInTheDocument, etc.) and
 * unmounts React trees after each test so tests don't leak DOM into each other.
 *
 * jsdom doesn't implement matchMedia (used by useTheme); stub it so components that
 * read the OS color-scheme preference don't blow up under test.
 */
afterEach(() => {
  cleanup();
});

if (!window.matchMedia) {
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}
