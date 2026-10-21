/*
 * Nombre: PrecioServiceTest.java
 * Descripción: Pruebas unitarias del cálculo de precio por incremento porcentual e historial (CP-005).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.producto.ProductoRepositories.HistorialPrecioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CA-03.1: actualización automática del precio de venta y registro en el historial.
 */
@ExtendWith(MockitoExtension.class)
class PrecioServiceTest {

    @Mock
    private HistorialPrecioRepository historialRepository;

    @InjectMocks
    private PrecioService service;

    @Test
    void calculaPrecioVentaDelContratoRest() {
        assertThat(PrecioService.calcularPrecioVenta(new BigDecimal("250000.00"), new BigDecimal("30.00")))
                .isEqualByComparingTo("325000.00");
    }

    @Test
    void calculaPrecioConPorcentajeDecimal() {
        assertThat(PrecioService.calcularPrecioVenta(new BigDecimal("1000"), new BigDecimal("12.5")))
                .isEqualByComparingTo("1125.0000");
    }

    @Test
    void costoPromedioPonderado() {
        // 10 u a 100 + 30 u a 200 = 7000 / 40 = 175
        assertThat(PrecioService.costoPromedio(new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("30"),
                new BigDecimal("200"))).isEqualByComparingTo("175");
    }

    @Test
    void costoPromedioSinExistenciasTomaElNuevoCosto() {
        assertThat(PrecioService.costoPromedio(BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("5"),
                new BigDecimal("80"))).isEqualByComparingTo("80");
    }

    @Test
    void rechazaCostoCeroOPorcentajeNegativo() {
        assertThatThrownBy(() -> PrecioService.validar(BigDecimal.ZERO, BigDecimal.TEN))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("precio.compra.invalido");
        assertThatThrownBy(() -> PrecioService.validar(BigDecimal.TEN, new BigDecimal("-1")))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("precio.porcentaje.invalido");
    }

    @Test
    void registraHistorialAlCambiarPorcentaje() {
        Producto p = producto("100", "20");
        service.aplicar(p, new BigDecimal("100"), new BigDecimal("50"), HistorialPrecio.Motivo.ACTUALIZACION_MANUAL, null);

        ArgumentCaptor<HistorialPrecio> captor = ArgumentCaptor.forClass(HistorialPrecio.class);
        verify(historialRepository).save(captor.capture());
        HistorialPrecio h = captor.getValue();
        assertThat(h.getPrecioVentaAnterior()).isEqualByComparingTo("120");
        assertThat(h.getPrecioVentaNuevo()).isEqualByComparingTo("150");
        assertThat(p.getPrecioVenta()).isEqualByComparingTo("150");
    }

    @Test
    void noRegistraHistorialSiNoHayCambios() {
        Producto p = producto("100", "20");
        service.aplicar(p, new BigDecimal("100.0000"), new BigDecimal("20.00"), HistorialPrecio.Motivo.COMPRA, "DI-1");
        verify(historialRepository, never()).save(any());
    }

    private static Producto producto(String costo, String pct) {
        Producto p = new Producto();
        p.setPrecioCompra(new BigDecimal(costo));
        p.setPorcentajeIncremento(new BigDecimal(pct));
        p.setPrecioVenta(PrecioService.calcularPrecioVenta(p.getPrecioCompra(), p.getPorcentajeIncremento()));
        return p;
    }
}
