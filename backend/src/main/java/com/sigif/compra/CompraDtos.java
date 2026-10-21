/*
 * Nombre: CompraDtos.java
 * Descripción: DTOs JSON de entradas por compra y Documento de Ingreso.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.compra;

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

/**
 * Registros del módulo de compras.
 */
public final class CompraDtos {

    private CompraDtos() {
    }

    /**
     * @param proveedorId            proveedor
     * @param numeroFacturaProveedor factura del proveedor
     * @param observaciones          observaciones
     * @param lineas                 productos comprados
     */
    public record CompraRequest(@NotNull(message = "{validacion.obligatorio}") Long proveedorId,
                                @Size(max = 40, message = "{validacion.longitud}") String numeroFacturaProveedor,
                                @Size(max = 500, message = "{validacion.longitud}") String observaciones,
                                @NotEmpty(message = "{validacion.lineas}") @Valid List<LineaCompra> lineas) {
    }

    /**
     * @param productoId       producto
     * @param unidadId         unidad del documento (null = unidad base)
     * @param cantidad         cantidad (&gt; 0)
     * @param costoUnitario    costo por unidad del documento (&gt; 0)
     * @param porcentajeIva    IVA de la compra
     * @param codigoLote       lote (obligatorio si el producto maneja vencimiento)
     * @param fechaVencimiento vencimiento del lote
     */
    public record LineaCompra(@NotNull(message = "{validacion.obligatorio}") Long productoId,
                              Long unidadId,
                              @NotNull(message = "{validacion.obligatorio}")
                              @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}")
                              BigDecimal cantidad,
                              @NotNull(message = "{validacion.obligatorio}")
                              @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}")
                              BigDecimal costoUnitario,
                              @DecimalMin(value = "0", message = "{validacion.no.negativo}")
                              @DecimalMax(value = "100", message = "{validacion.maximo.cien}") BigDecimal porcentajeIva,
                              @Size(max = 40, message = "{validacion.longitud}") String codigoLote,
                              LocalDate fechaVencimiento) {
    }

    /**
     * @param id                     id
     * @param numero                 número DI-XXXXXXXX
     * @param fecha                  fecha
     * @param proveedor              proveedor
     * @param numeroFacturaProveedor factura del proveedor
     * @param subtotal               subtotal
     * @param totalIva               IVA
     * @param total                  total
     * @param observaciones          observaciones
     * @param usuario                responsable
     * @param detalles               líneas
     */
    public record DocumentoIngresoResponse(Long id, String numero, OffsetDateTime fecha, TerceroResumen proveedor,
                                           String numeroFacturaProveedor, BigDecimal subtotal, BigDecimal totalIva,
                                           BigDecimal total, String observaciones, String usuario,
                                           List<DetalleIngresoResponse> detalles) {

        /**
         * @param d documento
         * @return DTO con detalle
         */
        public static DocumentoIngresoResponse de(DocumentoIngreso d) {
            return new DocumentoIngresoResponse(d.getId(), d.getNumero(), d.getFecha(), TerceroResumen.de(d.getProveedor()),
                    d.getNumeroFacturaProveedor(), d.getSubtotal(), d.getTotalIva(), d.getTotal(), d.getObservaciones(),
                    d.getUsuario(), d.getDetalles().stream().map(DetalleIngresoResponse::de).toList());
        }

        /**
         * @param d documento
         * @return DTO sin detalle (listados)
         */
        public static DocumentoIngresoResponse resumen(DocumentoIngreso d) {
            return new DocumentoIngresoResponse(d.getId(), d.getNumero(), d.getFecha(), TerceroResumen.de(d.getProveedor()),
                    d.getNumeroFacturaProveedor(), d.getSubtotal(), d.getTotalIva(), d.getTotal(), d.getObservaciones(),
                    d.getUsuario(), List.of());
        }
    }

    /**
     * @param productoId       producto
     * @param codigoBarras     código
     * @param producto         nombre
     * @param unidad           unidad del documento
     * @param cantidad         cantidad
     * @param cantidadBase     cantidad en unidad base
     * @param costoUnitario    costo
     * @param porcentajeIva    IVA
     * @param subtotal         subtotal
     * @param valorIva         IVA
     * @param total            total
     * @param codigoLote       lote
     * @param fechaVencimiento vencimiento
     */
    public record DetalleIngresoResponse(Long productoId, String codigoBarras, String producto, String unidad,
                                         BigDecimal cantidad, BigDecimal cantidadBase, BigDecimal costoUnitario,
                                         BigDecimal porcentajeIva, BigDecimal subtotal, BigDecimal valorIva,
                                         BigDecimal total, String codigoLote, LocalDate fechaVencimiento) {

        static DetalleIngresoResponse de(DetalleIngreso d) {
            return new DetalleIngresoResponse(d.getProducto().getId(), d.getProducto().getCodigoBarras(),
                    d.getProducto().getNombre(), d.getUnidad().getCodigo(), d.getCantidad(), d.getCantidadBase(),
                    d.getCostoUnitario(), d.getPorcentajeIva(), d.getSubtotal(), d.getValorIva(), d.getTotal(),
                    d.getLote() == null ? null : d.getLote().getCodigoLote(),
                    d.getLote() == null ? null : d.getLote().getFechaVencimiento());
        }
    }
}
