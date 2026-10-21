/*
 * Nombre: ConflictoException.java
 * Descripción: Excepción para conflictos de estado o unicidad (HTTP 409).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Se lanza ante duplicados (código de barras, documento) o conflictos de inventario
 * (stock insuficiente, periodo cerrado).
 */
public class ConflictoException extends SigifException {

    /**
     * @param clave      clave del mensaje i18n
     * @param argumentos argumentos del mensaje
     */
    public ConflictoException(String clave, Object... argumentos) {
        super(HttpStatus.CONFLICT, clave, argumentos);
    }
}
