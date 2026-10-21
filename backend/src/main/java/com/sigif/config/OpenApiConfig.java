/*
 * Nombre: OpenApiConfig.java
 * Descripción: Documentación OpenAPI 3 / Swagger UI del contrato REST.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define metadatos del API y los esquemas de seguridad (JWT Bearer y API Key).
 */
@Configuration
public class OpenApiConfig {

    /**
     * @return definición OpenAPI
     */
    @Bean
    public OpenAPI sigifOpenApi() {
        return new OpenAPI()
                .info(new Info().title("SIGIF API").version("1.0.0")
                        .description("Sistema Integral de Gestión de Inventario, Compras y Ventas para Ferretería"))
                .components(new Components()
                        .addSecuritySchemes("bearer", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT"))
                        .addSecuritySchemes("apiKey", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-API-Key")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
