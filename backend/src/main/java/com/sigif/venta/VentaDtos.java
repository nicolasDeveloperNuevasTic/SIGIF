/*
 * Nombre: VentaDtos.java
 * Descripción: DTOs JSON de salidas por venta y Factura.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.venta;

import com.sigif.tercero.TerceroDtos.TerceroResumen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registros del módulo de ventas.
 */
public final class VentaDtos {

    private VentaDtos() {
    }

    /**
     * @param clienteId     cliente (null = Consumidor Final)
     * @param observaciones observaciones
     * @param lineas        productos vendidos
     */
    public record VentaRequest(Long clienteId,
                               @Size(max = 500, message = "{validacion.longitud}") String observaciones,
                               @NotEmpty(message = "{validacion.lineas}") @Valid List<LineaVenta> lineas) {
    }

    /**
     * @param productoId          producto
     * @param unidadId            unidad (null = unidad base)
     * @param cantidad            cantidad entera o fracción (&gt; 0)
     * @param porcentajeDescuento descuento de la línea
     * @param reservaId           reserva a consumir (opcional)
     * @param numeroSerie         serial para garantía (opcional)
     */
    public record LineaVenta(@NotNull(message = "{validacion.obligatorio}") Long productoId,
                             Long unidadId,
                             @NotNull(message = "{validacion.obligatorio}")
                             @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}")
                             BigDecimal cantidad,
                             @DecimalMin(value = "0", message = "{validacion.no.negativo}")
                             @DecimalMax(value = "100", message = "{validacion.maximo.cien}")
                             BigDecimal porcentajeDescuento,
                             Long reservaId,
                             @Size(max = 60, message = "{validacion.longitud}") String numeroSerie) {
    }

    /**
     * @param id             id
     * @param numero         número FV-XXXXXXXX
     * @param fecha          fecha
     * @param cliente        cliente
     * @param subtotal       subtotal
     * @param totalDescuento descuentos
     * @param totalIva       IVA
     * @param total          total
     * @param origen         EN_LINEA / OFFLINE / API
     * @param idLocalOffline id local si vino del modo offline
     * @param observaciones  observaciones
     * @param usuario        vendedor
     * @param detalles       líneas
     */
    public record FacturaResponse(Long id, String numero, OffsetDateTime fecha, TerceroResumen cliente,
                                  BigDecimal subtotal, BigDecimal totalDescuento, BigDecimal totalIva, BigDecimal total,
                                  String origen, UUID idLocalOffline, String observaciones, String usuario,
                                  List<DetalleFacturaResponse> detalles) {

        /**
         * @param f factura
         * @return DTO con detalle
         */
        public static FacturaResponse de(Factura f) {
            return new FacturaResponse(f.getId(), f.getNumero(), f.getFecha(), TerceroResumen.de(f.getCliente()),
                    f.getSubtotal(), f.getTotalDescuento(), f.getTotalIva(), f.getTotal(), f.getOrigen().name(),
                    f.getIdLocalOffline(), f.getObservaciones(), f.getUsuario(),
                    f.getDetalles().stream().map(DetalleFacturaResponse::de).toList());
        }

        /**
         * @param f factura
         * @return DTO sin detalle
         */
        public static FacturaResponse resumen(Factura f) {
            return new FacturaResponse(f.getId(), f.getNumero(), f.getFecha(), TerceroResumen.de(f.getCliente()),
                    f.getSubtotal(), f.getTotalDescuento(), f.getTotalIva(), f.getTotal(), f.getOrigen().name(),
                    f.getIdLocalOffline(), f.getObservaciones(), f.getUsuario(), List.of());
        }
    }

    /**
     * @param productoId          producto
     * @param codigoBarras        código
     * @param producto            nombre
     * @param unidad              unidad
     * @param cantidad            cantidad
     * @param cantidadBase        cantidad base descontada
     * @param precioUnitario      precio por unidad del documento
     * @param porcentajeDescuento descuento
     * @param porcentajeIva       IVA
     * @param subtotal            subtotal
     * @param valorDescuento      descuento
     * @param valorIva            IVA
     * @param total               total
     * @param mesesGarantia       meses de garantía
     * @param fechaFinGarantia    fin de garantía
     * @param numeroSerie         serial
     * @param reservaId           reserva consumida
     */
    public record DetalleFacturaResponse(Long productoId, String codigoBarras, String producto, String unidad,
                                         BigDecimal cantidad, BigDecimal cantidadBase, BigDecimal precioUnitario,
                                         BigDecimal porcentajeDescuento, BigDecimal porcentajeIva, BigDecimal subtotal,
                                         BigDecimal valorDescuento, BigDecimal valorIva, BigDecimal total,
                                         Integer mesesGarantia, LocalDate fechaFinGarantia, String numeroSerie,
                                         Long reservaId) {

        static DetalleFacturaResponse de(DetalleFactura d) {
            return new DetalleFacturaResponse(d.getProducto().getId(), d.getProducto().getCodigoBarras(),
                    d.getProducto().getNombre(), d.getUnidad().getCodigo(), d.getCantidad(), d.getCantidadBase(),
                    d.getPrecioUnitario(), d.getPorcentajeDescuento(), d.getPorcentajeIva(), d.getSubtotal(),
                    d.getValorDescuento(), d.getValorIva(), d.getTotal(), d.getMesesGarantia(), d.getFechaFinGarantia(),
                    d.getNumeroSerie(), d.getReserva() == null ? null : d.getReserva().getId());
        }
    }
}
