/*
 * Nombre: RecursoNoEncontradoException.java
 * Descripción: Excepción para recursos inexistentes (HTTP 404).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un recurso solicitado no existe.
 */
public class RecursoNoEncontradoException extends SigifException {

    /**
     * @param recurso nombre lógico del recurso (clave i18n, ej. "recurso.producto")
     * @param id      identificador buscado
     */
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(HttpStatus.NOT_FOUND, "error.recurso.no.encontrado", recurso, id);
    }
}
