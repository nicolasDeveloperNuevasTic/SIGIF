/*
 * Nombre: ConversionServiceTest.java
 * Descripción: Pruebas unitarias de fracciones y conversiones de unidades (CP-004).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sigif.catalogo.CatalogoRepositories.UnidadMedidaRepository;
import com.sigif.catalogo.UnidadMedida;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.producto.ConversionService.Conversion;
import com.sigif.producto.ConversionService.Uso;
import com.sigif.producto.ProductoRepositories.ConversionUnidadRepository;
import com.sigif.producto.ProductoRepositories.ProductoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CA-02.1: venta de 2.5 metros de un rollo de 100 metros descontando la proporción exacta.
 */
@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock
    private ConversionUnidadRepository conversionRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UnidadMedidaRepository unidadRepository;

    @InjectMocks
    private ConversionService service;

    private Producto cable;
    private UnidadMedida metro;
    private UnidadMedida rollo;

    @BeforeEach
    void preparar() {
        metro = unidad(1L, "MT");
        rollo = unidad(2L, "ROLLO");
        cable = new Producto();
        cable.setId(10L);
        cable.setNombre("Cable 12 AWG");
        cable.setUnidadBase(metro);
        cable.setPermiteFraccion(true);
    }

    @Test
    void ventaFraccionadaEnUnidadBase() {
        Conversion c = service.convertir(cable, null, new BigDecimal("2.5"), Uso.VENTA);
        assertThat(c.cantidadBase()).isEqualByComparingTo("2.5");
        assertThat(c.factor()).isEqualByComparingTo("1");
    }

    @Test
    void compraPorRolloSeConvierteAMetros() {
        ConversionUnidad conv = new ConversionUnidad();
        conv.setUnidad(rollo);
        conv.setFactor(new BigDecimal("100.000000"));
        when(conversionRepository.findByProductoIdAndUnidadId(10L, 2L)).thenReturn(Optional.of(conv));

        Conversion c = service.convertir(cable, 2L, BigDecimal.ONE, Uso.COMPRA);
        assertThat(c.cantidadBase()).isEqualByComparingTo("100");
        // 100 - 2.5 = 97.5 exactos: sin errores de redondeo en BigDecimal
        assertThat(c.cantidadBase().subtract(new BigDecimal("2.5"))).isEqualByComparingTo("97.5");
    }

    @Test
    void rechazaFraccionEnProductoNoFraccionable() {
        cable.setPermiteFraccion(false);
        assertThatThrownBy(() -> service.convertir(cable, null, new BigDecimal("0.5"), Uso.VENTA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("producto.no.fraccionable");
    }

    @Test
    void rechazaMasDecimalesDeLosPermitidos() {
        assertThatThrownBy(() -> service.convertir(cable, null, new BigDecimal("1.2345"), Uso.VENTA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("cantidad.decimales");
    }

    @Test
    void rechazaUnidadSinConversion() {
        when(conversionRepository.findByProductoIdAndUnidadId(10L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.convertir(cable, 99L, BigDecimal.ONE, Uso.VENTA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("conversion.no.configurada");
    }

    @Test
    void rechazaCantidadNoPositiva() {
        assertThatThrownBy(() -> service.convertir(cable, null, BigDecimal.ZERO, Uso.VENTA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("cantidad.invalida");
    }

    private static UnidadMedida unidad(Long id, String codigo) {
        UnidadMedida u = new UnidadMedida();
        u.setId(id);
        u.setCodigo(codigo);
        return u;
    }
}
