/*
 * Nombre: JpaConfig.java
 * Descripción: Habilita repositorios Spring Data JPA, incluidos los declarados como interfaces anidadas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Escanea {@code com.sigif} buscando repositorios, permitiendo agrupar repositorios pequeños
 * de un mismo módulo en una sola clase contenedora.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.sigif", considerNestedRepositories = true)
public class JpaConfig {
}
