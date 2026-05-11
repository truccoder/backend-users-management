package com.backend.users.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI openAPI() {
    String securitySchemeName = "Bearer Authentication";
    return new OpenAPI()
        .info(
            new Info()
                .title("Backend Users Management API")
                .description(
                    "API for user management, authentication, friendships, and social connections")
                .version("1.0.0"))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .schemaRequirement(
            securitySchemeName,
            new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
  }
}
