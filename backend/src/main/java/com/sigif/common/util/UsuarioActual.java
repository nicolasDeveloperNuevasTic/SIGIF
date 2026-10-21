/*
 * Nombre: UsuarioActual.java
 * Descripción: Utilidad para obtener el usuario autenticado del contexto de seguridad.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso estático al nombre del usuario autenticado; devuelve {@code SISTEMA} para
 * procesos programados sin sesión (cierres, expiración de reservas).
 */
public final class UsuarioActual {

    /** Usuario usado por tareas automáticas. */
    public static final String SISTEMA = "SISTEMA";

    private UsuarioActual() {
    }

    /**
     * @return nombre del usuario autenticado o {@link #SISTEMA}
     */
    public static String nombre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return SISTEMA;
        }
        return auth.getName();
    }
}
