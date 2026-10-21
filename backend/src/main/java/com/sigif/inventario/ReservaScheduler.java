/*
 * Nombre: ReservaScheduler.java
 * Descripción: Tarea programada que libera reservas vencidas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada 15 minutos devuelve a DISPONIBLE las unidades de reservas vencidas. Cada reserva se
 * procesa en su propia transacción para que un error no detenga las demás.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaScheduler {

    private final ReservaService reservaService;

    /**
     * Expira las reservas activas cuya fecha de expiración ya pasó.
     */
    @Scheduled(cron = "${sigif.reservas.cron-expiracion}", zone = "${sigif.zona-horaria}")
    public void expirarReservas() {
        for (Long id : reservaService.vencidas()) {
            try {
                reservaService.expirar(id);
            } catch (RuntimeException ex) {
                log.warn("No fue posible expirar la reserva {}: {}", id, ex.getMessage());
            }
        }
    }
}
