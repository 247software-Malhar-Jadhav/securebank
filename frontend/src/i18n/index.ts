import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import en from "./locales/en/translation.json";
import hi from "./locales/hi/translation.json";
import mr from "./locales/mr/translation.json";

/**
 * react-i18next wiring for SecureBank.
 *
 * Why it is set up this way:
 * - We bundle the three supported locales (en/hi/mr) as static JSON so the UI has
 *   zero-latency translations and works offline. The backend ALSO localizes its own
 *   messages (validation/errors) via Accept-Language — see services/api.ts — so the
 *   user sees one consistent language across both layers.
 * - LanguageDetector decides the initial language: it checks localStorage first (so a
 *   returning user keeps their choice), then the browser's navigator.language, then
 *   falls back to `en`. The chosen language is persisted back to localStorage under
 *   the key below by the detector's caching.
 * - `supportedLngs` + `nonExplicitSupportedLngs` collapse region variants like "en-US"
 *   down to "en" so detection from the browser maps cleanly onto our three bundles.
 * - i18next escapes by default for many frameworks, but React already escapes JSX, so
 *   we disable interpolation escaping to avoid double-encoding.
 */
export const SUPPORTED_LOCALES = ["en", "hi", "mr"] as const;

export const LOCALE_LABELS: Record<(typeof SUPPORTED_LOCALES)[number], string> = {
  en: "English",
  hi: "हिन्दी",
  mr: "मराठी",
};

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      hi: { translation: hi },
      mr: { translation: mr },
    },
    fallbackLng: "en",
    supportedLngs: SUPPORTED_LOCALES as unknown as string[],
    nonExplicitSupportedLngs: true,
    load: "languageOnly",
    interpolation: {
      escapeValue: false, // React already protects against XSS in rendered values.
    },
    detection: {
      order: ["localStorage", "navigator", "htmlTag"],
      lookupLocalStorage: "securebank.lang",
      caches: ["localStorage"],
    },
  });

export default i18n;
