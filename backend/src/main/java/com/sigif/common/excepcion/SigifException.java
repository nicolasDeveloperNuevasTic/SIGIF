/*
 * Nombre: SigifException.java
 * Descripción: Excepción base de negocio con estado HTTP y clave de mensaje i18n.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.excepcion;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio que transporta el código HTTP de respuesta y la clave del
 * mensaje internacionalizado (resuelto según la cabecera Accept-Language).
 */
@Getter
public class SigifException extends RuntimeException {

    private final HttpStatus estado;
    private final String clave;
    private final transient Object[] argumentos;

    /**
     * @param estado     estado HTTP a devolver
     * @param clave      clave del mensaje en i18n/messages
     * @param argumentos argumentos de sustitución del mensaje
     */
    public SigifException(HttpStatus estado, String clave, Object... argumentos) {
        super(clave);
        this.estado = estado;
        this.clave = clave;
        this.argumentos = argumentos;
    }
}
