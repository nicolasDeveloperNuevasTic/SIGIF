/*
 * Nombre: ConsecutivoService.java
 * Descripción: Numeración única y secuencial de Facturas y Documentos de Ingreso.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.numeracion;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Obtiene el siguiente número con {@code UPDATE ... RETURNING}, que bloquea la fila del
 * consecutivo hasta el fin de la transacción: no hay huecos ni duplicados.
 */
@Service
@RequiredArgsConstructor
public class ConsecutivoService {

    /** Consecutivo de facturas. */
    public static final String FACTURA = "FACTURA";
    /** Consecutivo de documentos de ingreso. */
    public static final String DOC_INGRESO = "DOC_INGRESO";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param codigo FACTURA o DOC_INGRESO
     * @return número formateado, ej. FV-00000001
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente(String codigo) {
        return jdbcTemplate.queryForObject(
                "UPDATE consecutivo SET siguiente = siguiente + 1 WHERE codigo = ? "
                        + "RETURNING prefijo || '-' || lpad((siguiente - 1)::text, 8, '0')",
                String.class, codigo);
    }
}
