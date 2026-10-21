/*
 * Nombre: MigracionDtos.java
 * Descripción: DTOs del resultado de la migración desde Excel.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.migracion;

import java.util.List;

/**
 * Registros del módulo de migración.
 */
public final class MigracionDtos {

    private MigracionDtos() {
    }

    /**
     * @param hoja  hoja del libro
     * @param fila  número de fila (como se ve en Excel)
     * @param causa causa del rechazo
     */
    public record ErrorFila(String hoja, int fila, String causa) {
    }

    /**
     * @param hoja      hoja
     * @param leidas    filas con datos
     * @param importadas filas importadas
     * @param rechazadas filas rechazadas
     */
    public record ResumenHoja(String hoja, int leidas, int importadas, int rechazadas) {
    }

    /**
     * Informe de importación (CA-11.1).
     *
     * @param archivo nombre del archivo
     * @param hojas   resumen por hoja
     * @param errores filas rechazadas con su causa
     */
    public record ResultadoMigracion(String archivo, List<ResumenHoja> hojas, List<ErrorFila> errores) {
    }
}
