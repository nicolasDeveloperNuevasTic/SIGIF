/*
 * Nombre: PeriodoService.java
 * Descripción: Cálculo del periodo contable (AAAAMM) y validación de periodos cerrados.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

import com.sigif.common.excepcion.ConflictoException;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Los periodos se expresan como enteros AAAAMM en la zona horaria del negocio. Un periodo con
 * cierre registrado no admite movimientos (RN-011); la regla también la garantiza un trigger.
 */
@Service
@RequiredArgsConstructor
public class PeriodoService {

    private final CierreMensualRepository cierreRepository;

    @Value("${sigif.zona-horaria:America/Bogota}")
    private String zona;

    /**
     * @return zona horaria del negocio
     */
    public ZoneId zona() {
        return ZoneId.of(zona);
    }

    /**
     * @param fecha fecha
     * @return periodo AAAAMM
     */
    public int periodo(OffsetDateTime fecha) {
        YearMonth ym = YearMonth.from(fecha.atZoneSameInstant(zona()));
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    /**
     * @return periodo actual
     */
    public int periodoActual() {
        return periodo(OffsetDateTime.now());
    }

    /**
     * @param periodo periodo a validar
     */
    public void validarAbierto(int periodo) {
        if (cierreRepository.existsByPeriodo(periodo)) {
            throw new ConflictoException("cierre.periodo.cerrado", periodo);
        }
    }

    /**
     * @param periodo AAAAMM
     * @return periodo anterior
     */
    public static int anterior(int periodo) {
        YearMonth ym = YearMonth.of(periodo / 100, periodo % 100).minusMonths(1);
        return ym.getYear() * 100 + ym.getMonthValue();
    }
}
