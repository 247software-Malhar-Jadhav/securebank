import { useCallback, useEffect, useState } from "react";

type Theme = "light" | "dark";
const STORAGE_KEY = "securebank.theme";

/**
 * useTheme — light/dark switching.
 *
 * The whole palette is driven by a `.dark` class on <html> (see index.css), so this
 * hook only has to add/remove that class and remember the choice in localStorage. On
 * first load we honor a saved preference, otherwise the OS preference via matchMedia.
 */
export function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => {
    const saved = localStorage.getItem(STORAGE_KEY) as Theme | null;
    if (saved === "light" || saved === "dark") return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  });

  useEffect(() => {
    const root = document.documentElement;
    root.classList.toggle("dark", theme === "dark");
    localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((t) => (t === "dark" ? "light" : "dark"));
  }, []);

  return { theme, toggleTheme };
}
