/*
 * Nombre: ReglaNegocioException.java
 * Descripción: Excepción para violaciones de reglas de negocio con datos inválidos (HTTP 400).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando la petición es sintácticamente válida pero incumple una regla de negocio.
 */
public class ReglaNegocioException extends SigifException {

    /**
     * @param clave      clave del mensaje i18n
     * @param argumentos argumentos del mensaje
     */
    public ReglaNegocioException(String clave, Object... argumentos) {
        super(HttpStatus.BAD_REQUEST, clave, argumentos);
    }
}
