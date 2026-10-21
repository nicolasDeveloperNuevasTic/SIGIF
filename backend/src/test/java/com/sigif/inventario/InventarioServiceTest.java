/*
 * Nombre: InventarioServiceTest.java
 * Descripción: Pruebas unitarias de salidas por venta: bloqueo de unidades dañadas y vencidas, consumo FEFO (CA-04.2).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigif.cierre.PeriodoService;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.inventario.InventarioRepositories.InventarioSaldoRepository;
import com.sigif.inventario.InventarioRepositories.LoteRepository;
import com.sigif.inventario.InventarioRepositories.MovimientoInventarioRepository;
import com.sigif.producto.Producto;
import com.sigif.venta.Factura;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Reglas RN-007 y RN-009 en {@link InventarioService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventarioServiceTest {

    @Mock
    private InventarioSaldoRepository saldoRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private PeriodoService periodoService;

    @InjectMocks
    private InventarioService service;

    private Producto producto;
    private InventarioSaldo saldo;

    @BeforeEach
    void preparar() {
        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Pegante epóxico");
        producto.setPrecioCompra(new BigDecimal("5000"));
        saldo = new InventarioSaldo();
        saldo.setProductoId(1L);
        saldo.setDisponible(new BigDecimal("10"));
        saldo.setDanada(new BigDecimal("4"));
        when(saldoRepository.bloquear(1L)).thenReturn(Optional.of(saldo));
        when(periodoService.zona()).thenReturn(ZoneId.of("America/Bogota"));
        when(periodoService.periodo(any())).thenReturn(202610);
    }

    @Test
    void noPermiteVenderUnidadesDanadas() {
        assertThatThrownBy(() -> service.registrarSalida(producto, BigDecimal.ONE, EstadoInventario.DANADA, new Factura(),
                null, OffsetDateTime.now()))
                .isInstanceOf(ConflictoException.class).hasMessage("inventario.venta.danada");
        assertThat(saldo.getDanada()).isEqualByComparingTo("4");
    }

    @Test
    void noPermiteVenderSiSoloQuedanLotesVencidos() {
        producto.setTieneVencimiento(true);
        when(loteRepository.bloquearVigentes(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        assertThatThrownBy(() -> service.registrarSalida(producto, BigDecimal.ONE, EstadoInventario.DISPONIBLE,
                new Factura(), null, OffsetDateTime.now()))
                .isInstanceOf(ConflictoException.class).hasMessage("inventario.vencido");
    }

    @Test
    void consumeLotesEnOrdenFefo() {
        producto.setTieneVencimiento(true);
        Lote primero = lote("L1", "3");
        Lote segundo = lote("L2", "5");
        when(loteRepository.bloquearVigentes(eq(1L), any(LocalDate.class))).thenReturn(List.of(primero, segundo));

        service.registrarSalida(producto, new BigDecimal("4"), EstadoInventario.DISPONIBLE, new Factura(), null,
                OffsetDateTime.now());

        assertThat(primero.getCantidadActual()).isEqualByComparingTo("0");
        assertThat(segundo.getCantidadActual()).isEqualByComparingTo("4");
        assertThat(saldo.getDisponible()).isEqualByComparingTo("6");
        verify(movimientoRepository, times(2)).save(any());
    }

    private static Lote lote(String codigo, String cantidad) {
        Lote l = new Lote();
        l.setCodigoLote(codigo);
        l.setFechaVencimiento(LocalDate.now().plusMonths(2));
        l.setCantidadActual(new BigDecimal(cantidad));
        return l;
    }
}
