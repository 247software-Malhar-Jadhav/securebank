package com.securebank.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Serves a backend message bundle as JSON for a requested locale
 * ({@code GET /i18n/{locale}}).
 *
 * <p>The frontend uses this so backend-originated strings (notification
 * templates, error messages) can be rendered consistently with the UI's own
 * translations. We read the {@code messages_<locale>.properties} bundle directly
 * via {@link ResourceBundle} and return it as a flat key->value map. Unsupported
 * locales fall back to English.</p>
 */
@RestController
@RequestMapping("/i18n")
@Tag(name = "i18n", description = "Backend message bundles for the frontend")
public class I18nController {

    private static final Set<String> SUPPORTED = Set.of("en", "hi", "mr");

    @Operation(summary = "Get the backend message bundle for a locale (en/hi/mr)")
    @GetMapping("/{locale}")
    public Map<String, String> bundle(@PathVariable String locale) {
        String lang = SUPPORTED.contains(locale) ? locale : "en";
        // Load via ResourceBundle so encoding (UTF-8 for Devanagari) and key
        // resolution match the MessageSource exactly.
        ResourceBundle rb = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag(lang));
        Map<String, String> out = new TreeMap<>();
        for (String key : rb.keySet()) {
            out.put(key, rb.getString(key));
        }
        return out;
    }
}
