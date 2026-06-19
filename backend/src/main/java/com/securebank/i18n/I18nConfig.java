package com.securebank.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Internationalization wiring.
 *
 * <p>Two beans matter here:</p>
 * <ul>
 *   <li>{@link MessageSource} - loads {@code i18n/messages_*.properties} and
 *       resolves keys to localized text. Configured UTF-8 so Devanagari (hi/mr)
 *       renders correctly.</li>
 *   <li>{@link LocaleResolver} - an {@link AcceptHeaderLocaleResolver} that picks
 *       the request locale from the {@code Accept-Language} header, restricted to
 *       the three supported locales with English as the default. This is what
 *       drives localized validation messages and RFC-7807 error bodies.</li>
 * </ul>
 */
@Configuration
public class I18nConfig {

    /** Supported locales, default first. Kept in one place for reuse by the controller. */
    public static final List<Locale> SUPPORTED = List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("hi"),
            Locale.forLanguageTag("mr"));

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:i18n/messages");
        ms.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // If a key is missing, return the key itself rather than throwing - the
        // error handler must never blow up while building an error response.
        ms.setUseCodeAsDefaultMessage(true);
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(SUPPORTED);
        return resolver;
    }
}
