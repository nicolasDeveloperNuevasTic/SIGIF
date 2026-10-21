/*
 * Nombre: ProductoDtos.java
 * Descripción: Contrato JSON de la entidad Producto (ProductoRequest / ProductoResponse) y auxiliares.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Registros del contrato REST de productos (sección 9.3 del SRS).
 */
public final class ProductoDtos {

    private ProductoDtos() {
    }

    /**
     * Petición de creación/actualización.
     *
     * @param nombre                nombre
     * @param categoriaId           categoría
     * @param descripcion           descripción
     * @param codigoBarras          código de barras único
     * @param marcaId               marca
     * @param presentacionId        presentación
     * @param unidadBaseId          unidad base de inventario
     * @param permiteFraccion       admite venta fraccionada
     * @param precioCompra          costo de compra por unidad base (&gt; 0)
     * @param porcentajeIncremento  porcentaje de incremento (&gt;= 0)
     * @param porcentajeIva         IVA aplicado en ventas
     * @param tieneGarantia         maneja garantía
     * @param mesesGarantia         meses de garantía
     * @param tieneVencimiento      maneja vencimiento (control por lotes)
     * @param diasAlertaVencimiento días de anticipación de la alerta
     * @param stockMinimo           stock mínimo de alerta
     */
    public record ProductoRequest(
            @NotBlank(message = "{validacion.obligatorio}") @Size(max = 200, message = "{validacion.longitud}")
            String nombre,
            @NotNull(message = "{validacion.obligatorio}") Long categoriaId,
            @Size(max = 2000, message = "{validacion.longitud}") String descripcion,
            @NotBlank(message = "{validacion.obligatorio}")
            @Pattern(regexp = "^[0-9A-Za-z-]{4,50}$", message = "{validacion.codigo.barras}") String codigoBarras,
            @NotNull(message = "{validacion.obligatorio}") Long marcaId,
            @NotNull(message = "{validacion.obligatorio}") Long presentacionId,
            @NotNull(message = "{validacion.obligatorio}") Long unidadBaseId,
            boolean permiteFraccion,
            @NotNull(message = "{validacion.obligatorio}")
            @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}")
            @Digits(integer = 14, fraction = 4, message = "{validacion.decimales}") BigDecimal precioCompra,
            @NotNull(message = "{validacion.obligatorio}")
            @PositiveOrZero(message = "{validacion.no.negativo}")
            @Digits(integer = 5, fraction = 2, message = "{validacion.decimales}") BigDecimal porcentajeIncremento,
            @DecimalMin(value = "0", message = "{validacion.no.negativo}")
            @DecimalMax(value = "100", message = "{validacion.maximo.cien}") BigDecimal porcentajeIva,
            boolean tieneGarantia,
            @Min(value = 1, message = "{validacion.positivo}") @Max(value = 240, message = "{validacion.longitud}")
            Integer mesesGarantia,
            boolean tieneVencimiento,
            @Min(value = 0, message = "{validacion.no.negativo}") Integer diasAlertaVencimiento,
            @PositiveOrZero(message = "{validacion.no.negativo}") BigDecimal stockMinimo) {
    }

    /**
     * Existencias por estado.
     *
     * @param disponible disponible
     * @param reservada  reservada
     * @param danada     dañada
     * @param total      total físico
     */
    public record Existencias(BigDecimal disponible, BigDecimal reservada, BigDecimal danada, BigDecimal total) {
    }

    /**
     * Respuesta del contrato REST.
     *
     * @param id                    id
     * @param nombre                nombre
     * @param categoria             nombre de la categoría
     * @param descripcion           descripción
     * @param estado                ACTIVO | INACTIVO
     * @param codigoBarras          código de barras
     * @param marca                 nombre de la marca
     * @param presentacion          nombre de la presentación
     * @param permiteFraccion       admite fracción
     * @param precioCompra          costo (promedio ponderado)
     * @param porcentajeIncremento  porcentaje
     * @param precioVentaCalculado  precio de venta
     * @param tieneGarantia         garantía
     * @param mesesGarantia         meses
     * @param tieneVencimiento      vencimiento
     * @param fechaVencimiento      próximo vencimiento con existencias
     * @param categoriaId           id categoría
     * @param marcaId               id marca
     * @param presentacionId        id presentación
     * @param unidadBaseId          id unidad base
     * @param unidadBase            código unidad base
     * @param porcentajeIva         IVA
     * @param diasAlertaVencimiento días de alerta
     * @param stockMinimo           stock mínimo
     * @param existencias           existencias por estado
     * @param actualizadoEn         última actualización
     */
    public record ProductoResponse(
            Long id, String nombre, String categoria, String descripcion, String estado, String codigoBarras,
            String marca, String presentacion, boolean permiteFraccion, BigDecimal precioCompra,
            BigDecimal porcentajeIncremento, BigDecimal precioVentaCalculado, boolean tieneGarantia,
            Integer mesesGarantia, boolean tieneVencimiento, LocalDate fechaVencimiento,
            Long categoriaId, Long marcaId, Long presentacionId, Long unidadBaseId, String unidadBase,
            BigDecimal porcentajeIva, int diasAlertaVencimiento, BigDecimal stockMinimo, Existencias existencias,
            OffsetDateTime actualizadoEn) {
    }

    /**
     * @param estado ACTIVO | INACTIVO
     */
    public record EstadoRequest(@NotNull(message = "{validacion.obligatorio}") EstadoProducto estado) {
    }

    /**
     * @param precioCompra         nuevo costo
     * @param porcentajeIncremento nuevo porcentaje
     */
    public record PrecioRequest(
            @NotNull(message = "{validacion.obligatorio}")
            @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}") BigDecimal precioCompra,
            @NotNull(message = "{validacion.obligatorio}")
            @PositiveOrZero(message = "{validacion.no.negativo}") BigDecimal porcentajeIncremento) {
    }

    /**
     * @param id                   id
     * @param precioCompraAnterior costo anterior
     * @param precioCompraNuevo    costo nuevo
     * @param porcentajeAnterior   porcentaje anterior
     * @param porcentajeNuevo      porcentaje nuevo
     * @param precioVentaAnterior  precio anterior
     * @param precioVentaNuevo     precio nuevo
     * @param motivo               motivo
     * @param referencia           documento
     * @param usuario              usuario
     * @param fecha                fecha
     */
    public record HistorialPrecioResponse(Long id, BigDecimal precioCompraAnterior, BigDecimal precioCompraNuevo,
                                          BigDecimal porcentajeAnterior, BigDecimal porcentajeNuevo,
                                          BigDecimal precioVentaAnterior, BigDecimal precioVentaNuevo,
                                          String motivo, String referencia, String usuario, OffsetDateTime fecha) {

        /**
         * @param h entidad
         * @return DTO
         */
        public static HistorialPrecioResponse de(HistorialPrecio h) {
            return new HistorialPrecioResponse(h.getId(), h.getPrecioCompraAnterior(), h.getPrecioCompraNuevo(),
                    h.getPorcentajeAnterior(), h.getPorcentajeNuevo(), h.getPrecioVentaAnterior(),
                    h.getPrecioVentaNuevo(), h.getMotivo().name(), h.getReferencia(), h.getUsuario(), h.getFecha());
        }
    }
}
