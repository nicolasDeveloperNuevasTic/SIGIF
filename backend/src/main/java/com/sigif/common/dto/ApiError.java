/*
 * Nombre: ApiError.java
 * Descripción: Estructura JSON estándar de error del contrato REST.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Respuesta de error internacionalizada.
 *
 * @param timestamp momento del error
 * @param estado    código HTTP
 * @param error     frase de razón HTTP
 * @param codigo    clave i18n del error (estable para clientes)
 * @param mensaje   mensaje traducido según Accept-Language
 * @param ruta      ruta solicitada
 * @param errores   errores por campo (validaciones)
 */
public record ApiError(
        OffsetDateTime timestamp,
        int estado,
        String error,
        String codigo,
        String mensaje,
        String ruta,
        List<ErrorCampo> errores) {

    /**
     * Error de validación de un campo.
     *
     * @param campo   nombre del campo
     * @param mensaje mensaje traducido
     */
    public record ErrorCampo(String campo, String mensaje) {
    }
}
