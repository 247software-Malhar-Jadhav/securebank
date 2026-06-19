package com.securebank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc OpenAPI metadata + the Bearer-JWT security scheme.
 *
 * <p>This bean populates the API title/version shown in Swagger UI and tells
 * springdoc that protected endpoints expect an {@code Authorization: Bearer
 * <jwt>} header, so the "Authorize" button in Swagger UI works out of the box.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI secureBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SecureBank API")
                        .version("1.0.0")
                        .description("Production-grade digital banking platform backend. "
                                + "Endpoints are prefixed with /api. Authenticate via "
                                + "/api/auth/login then pass the access token as a Bearer header.")
                        .contact(new Contact().name("SecureBank Engineering")
                                .email("engineering@securebank.local"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
