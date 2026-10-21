/*
 * Nombre: TransaccionConfig.java
 * Descripción: Orden de los proxies transaccionales respecto al aspecto de contexto de auditoría.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * La transacción se abre antes (orden más alto) que {@link AuditoriaContextoAspect}, de modo
 * que el aspecto ejecuta {@code set_config('sigif.usuario', ...)} dentro de la transacción
 * activa y los triggers registran el usuario de aplicación.
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE + 10)
public class TransaccionConfig {
}
