/*
 * Nombre: InventarioDtos.java
 * Descripción: DTOs JSON de existencias, movimientos, lotes, transiciones y reservas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Registros de entrada y salida del módulo de inventario.
 */
public final class InventarioDtos {

    private InventarioDtos() {
    }

    /**
     * @param productoId   producto
     * @param codigoBarras código
     * @param nombre       nombre
     * @param unidadBase   unidad base
     * @param disponible   disponible
     * @param reservada    reservada
     * @param danada       dañada
     * @param total        total
     * @param stockMinimo  stock mínimo
     * @param costoPromedio costo promedio
     * @param valor        valor total a costo
     */
    public record SaldoResponse(Long productoId, String codigoBarras, String nombre, String unidadBase,
                                BigDecimal disponible, BigDecimal reservada, BigDecimal danada, BigDecimal total,
                                BigDecimal stockMinimo, BigDecimal costoPromedio, BigDecimal valor) {

        /**
         * @param s saldo con producto cargado
         * @return DTO
         */
        public static SaldoResponse de(InventarioSaldo s) {
            var p = s.getProducto();
            return new SaldoResponse(s.getProductoId(), p.getCodigoBarras(), p.getNombre(),
                    p.getUnidadBase().getCodigo(), s.getDisponible(), s.getReservada(), s.getDanada(), s.total(),
                    p.getStockMinimo(), p.getPrecioCompra(),
                    s.total().multiply(p.getPrecioCompra()).setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    /**
     * @param productoId    producto
     * @param cantidad      cantidad en unidad base
     * @param estadoOrigen  estado origen
     * @param estadoDestino estado destino
     * @param motivo        motivo obligatorio
     */
    public record TransicionRequest(@NotNull(message = "{validacion.obligatorio}") Long productoId,
                                    @NotNull(message = "{validacion.obligatorio}")
                                    @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}")
                                    BigDecimal cantidad,
                                    @NotNull(message = "{validacion.obligatorio}") EstadoInventario estadoOrigen,
                                    @NotNull(message = "{validacion.obligatorio}") EstadoInventario estadoDestino,
                                    @NotBlank(message = "{validacion.obligatorio}")
                                    @Size(max = 300, message = "{validacion.longitud}") String motivo) {
    }

    /**
     * @param id            id
     * @param tipo          tipo
     * @param productoId    producto
     * @param producto      nombre
     * @param cantidad      cantidad base
     * @param estadoOrigen  origen
     * @param estadoDestino destino
     * @param costoUnitario costo
     * @param documento     número de documento soporte
     * @param lote          código de lote
     * @param motivo        motivo
     * @param usuario       usuario
     * @param fecha         fecha
     * @param periodo       periodo
     */
    public record MovimientoResponse(Long id, String tipo, Long productoId, String producto, BigDecimal cantidad,
                                     String estadoOrigen, String estadoDestino, BigDecimal costoUnitario,
                                     String documento, String lote, String motivo, String usuario,
                                     OffsetDateTime fecha, Integer periodo) {

        /**
         * @param m movimiento
         * @return DTO
         */
        public static MovimientoResponse de(MovimientoInventario m) {
            String doc = m.getFactura() != null ? m.getFactura().getNumero()
                    : m.getDocumentoIngreso() != null ? m.getDocumentoIngreso().getNumero()
                    : m.getReserva() != null ? "RES-" + m.getReserva().getId() : null;
            return new MovimientoResponse(m.getId(), m.getTipo().name(), m.getProducto().getId(),
                    m.getProducto().getNombre(), m.getCantidad(),
                    m.getEstadoOrigen() == null ? null : m.getEstadoOrigen().name(),
                    m.getEstadoDestino() == null ? null : m.getEstadoDestino().name(), m.getCostoUnitario(), doc,
                    m.getLote() == null ? null : m.getLote().getCodigoLote(), m.getMotivo(), m.getUsuario(),
                    m.getFecha(), m.getPeriodo());
        }
    }

    /**
     * @param id               id
     * @param productoId       producto
     * @param producto         nombre
     * @param codigoLote       lote
     * @param fechaVencimiento vencimiento
     * @param cantidadInicial  cantidad inicial
     * @param cantidadActual   cantidad actual
     * @param vencido          true si ya venció
     * @param diasParaVencer   días restantes (negativo si venció)
     */
    public record LoteResponse(Long id, Long productoId, String producto, String codigoLote,
                               LocalDate fechaVencimiento, BigDecimal cantidadInicial, BigDecimal cantidadActual,
                               boolean vencido, long diasParaVencer) {

        /**
         * @param l   lote
         * @param hoy fecha actual
         * @return DTO
         */
        public static LoteResponse de(Lote l, LocalDate hoy) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, l.getFechaVencimiento());
            return new LoteResponse(l.getId(), l.getProducto().getId(), l.getProducto().getNombre(), l.getCodigoLote(),
                    l.getFechaVencimiento(), l.getCantidadInicial(), l.getCantidadActual(), dias < 0, dias);
        }
    }

    /**
     * @param clienteId  cliente
     * @param productoId producto
     * @param cantidad   cantidad en unidad base
     * @param diasVigencia días de vigencia (por defecto configurable)
     * @param observaciones observaciones
     */
    public record ReservaRequest(@NotNull(message = "{validacion.obligatorio}") Long clienteId,
                                 @NotNull(message = "{validacion.obligatorio}") Long productoId,
                                 @NotNull(message = "{validacion.obligatorio}")
                                 @DecimalMin(value = "0", inclusive = false, message = "{validacion.positivo}")
                                 BigDecimal cantidad,
                                 @Min(value = 1, message = "{validacion.positivo}")
                                 @Max(value = 60, message = "{validacion.longitud}") Integer diasVigencia,
                                 @Size(max = 300, message = "{validacion.longitud}") String observaciones) {
    }

    /**
     * @param id                id
     * @param clienteId         cliente
     * @param cliente           nombre del cliente
     * @param productoId        producto
     * @param producto          nombre del producto
     * @param cantidad          cantidad reservada
     * @param cantidadPendiente pendiente por vender
     * @param fechaExpiracion   expiración
     * @param estado            estado
     * @param observaciones     observaciones
     * @param usuario           usuario
     * @param creadoEn          creación
     */
    public record ReservaResponse(Long id, Long clienteId, String cliente, Long productoId, String producto,
                                  BigDecimal cantidad, BigDecimal cantidadPendiente, OffsetDateTime fechaExpiracion,
                                  String estado, String observaciones, String usuario, OffsetDateTime creadoEn) {

        /**
         * @param r reserva
         * @return DTO
         */
        public static ReservaResponse de(Reserva r) {
            return new ReservaResponse(r.getId(), r.getCliente().getId(), r.getCliente().nombreMostrar(),
                    r.getProducto().getId(), r.getProducto().getNombre(), r.getCantidad(), r.getCantidadPendiente(),
                    r.getFechaExpiracion(), r.getEstado().name(), r.getObservaciones(), r.getUsuario(), r.getCreadoEn());
        }
    }
}
