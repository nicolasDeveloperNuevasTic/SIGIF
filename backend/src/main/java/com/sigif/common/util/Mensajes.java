/*
 * Nombre: Mensajes.java
 * Descripción: Resolución de mensajes internacionalizados según el locale de la petición.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Envoltorio de {@link MessageSource} que usa el locale resuelto desde Accept-Language.
 */
@Component
@RequiredArgsConstructor
public class Mensajes {

    private final MessageSource messageSource;

    /**
     * Traduce una clave. Si un argumento es a su vez una clave existente (ej. "recurso.producto")
     * también se traduce.
     *
     * @param clave      clave del mensaje
     * @param argumentos argumentos
     * @return mensaje traducido o la clave si no existe
     */
    public String obtener(String clave, Object... argumentos) {
        Object[] args = argumentos == null ? new Object[0] : argumentos.clone();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s && s.startsWith("recurso.")) {
                args[i] = messageSource.getMessage(s, null, s, LocaleContextHolder.getLocale());
            } else if (args[i] instanceof Number n) {
                // Evita separadores de miles en ids, periodos y cantidades (ej. 202610, no 202.610).
                args[i] = n instanceof java.math.BigDecimal d ? d.stripTrailingZeros().toPlainString() : n.toString();
            }
        }
        return messageSource.getMessage(clave, args, clave, LocaleContextHolder.getLocale());
    }
}
