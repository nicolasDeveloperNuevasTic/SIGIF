/*
 * Nombre: SigifApplication.java
 * Descripción: Punto de entrada del Back-End SIGIF (Spring Boot).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del Sistema Integral de Gestión de Inventario, Compras y Ventas
 * para Ferretería. Habilita la programación de tareas para cierres mensuales y
 * expiración de reservas.
 */
@SpringBootApplication
@EnableScheduling
public class SigifApplication {

    /**
     * Inicia la aplicación.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(SigifApplication.class, args);
    }
}
