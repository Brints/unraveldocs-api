package com.extractor.unraveldocs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class SwaggerConfig {
    @Value("${spring.application.version}")
    private String appVersion;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public GroupedOpenApi publicAPI() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .version(appVersion)
                        .description("API documentation for UnravelDocs, a platform that uses Tesseract OCR to extract text from images and PDFs, and provides a user-friendly interface for managing documents. Users can upload files all at once before processing or upload and extract at the same time, and the system supports various file formats including images and PDFs. The platform also includes features for user authentication, document management, and a rich text editor for editing extracted content. It is designed to be secure, efficient, and easy to use, with a focus on providing a seamless experience for users looking to extract and manage text from visual documents."))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local server"),
                        new Server()
                                .url("https://api.unraveldocs.xyz")
                                .description("Production server")
                ))
                .components(new Components()
                        .addSchemas("profilePicture", new Schema<>()
                                .type("string")
                                .format("binary")
                                .description("File upload"))
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))
                .security(Collections.singletonList(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                ));
    }
}
