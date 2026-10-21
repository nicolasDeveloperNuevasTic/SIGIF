/*
 * Nombre: CierreScheduler.java
 * Descripción: Programador automático del cierre mensual de inventario.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta el cierre del mes anterior según {@code sigif.cierre.cron} (por defecto el día 1 a las
 * 00:05). Si hay conflictos de sincronización cierra igualmente y deja la alerta registrada en
 * el cierre (REQ-F-029).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreScheduler {

    private final CierreService cierreService;
    private final PeriodoService periodoService;

    @Value("${sigif.cierre.habilitado:true}")
    private boolean habilitado;

    /**
     * Cierra el periodo anterior si aún no está cerrado.
     */
    @Scheduled(cron = "${sigif.cierre.cron}", zone = "${sigif.zona-horaria}")
    public void cerrarMesAnterior() {
        if (!habilitado) {
            return;
        }
        int periodo = PeriodoService.anterior(periodoService.periodoActual());
        if (cierreService.existe(periodo)) {
            log.info("El periodo {} ya está cerrado", periodo);
            return;
        }
        try {
            var cierre = cierreService.ejecutar(periodo, CierreMensual.TipoEjecucion.PROGRAMADO, true);
            if (cierre.operacionesPendientes() > 0) {
                log.warn("Cierre {} ejecutado con {} operaciones offline en conflicto", periodo,
                        cierre.operacionesPendientes());
            } else {
                log.info("Cierre mensual {} ejecutado. Valor inventario: {}", periodo, cierre.valorInventario());
            }
        } catch (RuntimeException ex) {
            log.error("Falló el cierre programado del periodo {}", periodo, ex);
        }
    }
}
