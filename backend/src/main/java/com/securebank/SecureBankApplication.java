package com.securebank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry point for the SecureBank backend.
 *
 * <ul>
 *   <li>{@code @SpringBootApplication} enables component scanning, auto-config,
 *       and the embedded server.</li>
 *   <li>{@code @ConfigurationPropertiesScan} registers our typed
 *       {@code SecureBankProperties} (and any others) without listing them by
 *       hand.</li>
 *   <li>{@code @EnableAsync} lets the Kafka->RabbitMQ bridge and after-commit
 *       event publishing run off the request thread.</li>
 * </ul>
 *
 * <p>Virtual threads for the web layer are switched on declaratively via
 * {@code spring.threads.virtual.enabled=true} in application.yml - there is no
 * Java code to write for Loom.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class SecureBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureBankApplication.class, args);
    }
}
