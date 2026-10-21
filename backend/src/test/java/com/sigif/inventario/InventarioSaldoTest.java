/*
 * Nombre: InventarioSaldoTest.java
 * Descripción: Pruebas unitarias de la prohibición de saldos negativos por estado (CA-04.1).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigif.common.excepcion.ConflictoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Valida la regla RN-008 en la entidad de saldo.
 */
class InventarioSaldoTest {

    @Test
    void permiteLlegarExactamenteACero() {
        InventarioSaldo s = new InventarioSaldo();
        s.ajustar(EstadoInventario.DISPONIBLE, new BigDecimal("5"), "Martillo");
        s.ajustar(EstadoInventario.DISPONIBLE, new BigDecimal("-5"), "Martillo");
        assertThat(s.getDisponible()).isEqualByComparingTo("0");
    }

    @Test
    void rechazaSaldoNegativo() {
        InventarioSaldo s = new InventarioSaldo();
        s.ajustar(EstadoInventario.DISPONIBLE, new BigDecimal("2"), "Martillo");
        assertThatThrownBy(() -> s.ajustar(EstadoInventario.DISPONIBLE, new BigDecimal("-2.0001"), "Martillo"))
                .isInstanceOf(ConflictoException.class).hasMessage("inventario.stock.insuficiente");
        assertThat(s.getDisponible()).isEqualByComparingTo("2");
    }

    @Test
    void estadosSonIndependientes() {
        InventarioSaldo s = new InventarioSaldo();
        s.ajustar(EstadoInventario.DISPONIBLE, new BigDecimal("40"), "Tornillo");
        s.ajustar(EstadoInventario.DISPONIBLE, new BigDecimal("-7"), "Tornillo");
        s.ajustar(EstadoInventario.RESERVADA, new BigDecimal("5"), "Tornillo");
        s.ajustar(EstadoInventario.DANADA, new BigDecimal("2"), "Tornillo");
        assertThat(s.getDisponible()).isEqualByComparingTo("33");
        assertThat(s.total()).isEqualByComparingTo("40");
        assertThatThrownBy(() -> s.ajustar(EstadoInventario.DANADA, new BigDecimal("-3"), "Tornillo"))
                .isInstanceOf(ConflictoException.class);
    }
}
