package com.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.url.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("Production-ready URL shortening service with analytics, authentication, and rate limiting. Create short URLs, track clicks, and manage links with ease.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@urlshortener.com")
                                .url("https://urlshortener.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(baseUrl)
                                .description("Server URL"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide a JWT token obtained from the /api/v1/auth/login or /api/v1/auth/register endpoint. Use the format: Bearer {token}")));
    }
}
