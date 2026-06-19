import { useTranslation } from "react-i18next";
import { Languages } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LOCALE_LABELS, SUPPORTED_LOCALES } from "@/i18n";

/**
 * LanguageSwitcher — lets the user pick en / हिन्दी / मराठी.
 *
 * Changing the language does two things at once:
 *  1. i18n.changeLanguage swaps every UI string instantly (and the detector
 *     persists the choice to localStorage).
 *  2. Because services/api.ts reads i18n.language for the Accept-Language header,
 *     subsequent API calls also come back localized — so backend validation/error
 *     messages match the UI language.
 */
export function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  // i18n.language may be a region variant ("en-US"); normalize to the base family.
  const current = (i18n.language || "en").split("-")[0];

  return (
    <Select value={current} onValueChange={(lng) => void i18n.changeLanguage(lng)}>
      <SelectTrigger
        className="w-[130px] gap-2"
        aria-label={t("language.select")}
      >
        <Languages className="h-4 w-4 opacity-70" />
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {SUPPORTED_LOCALES.map((lng) => (
          <SelectItem key={lng} value={lng}>
            {LOCALE_LABELS[lng]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
