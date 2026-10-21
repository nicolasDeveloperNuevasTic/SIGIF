/*
 * Nombre: SincronizacionDtos.java
 * Descripción: DTOs JSON del modo offline: lote de ventas a sincronizar, resultados y catálogo local.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.sincronizacion;

import com.sigif.venta.VentaDtos.VentaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registros del módulo de sincronización.
 */
public final class SincronizacionDtos {

    private SincronizacionDtos() {
    }

    /**
     * @param idLocal     UUID generado en el navegador
     * @param fechaLocal  fecha en que se registró sin conexión
     * @param dispositivo identificador del puesto de venta
     * @param venta       venta
     */
    public record VentaOffline(@NotNull(message = "{validacion.obligatorio}") UUID idLocal,
                               @NotNull(message = "{validacion.obligatorio}") OffsetDateTime fechaLocal,
                               @Size(max = 100, message = "{validacion.longitud}") String dispositivo,
                               @NotNull(message = "{validacion.obligatorio}") @Valid VentaRequest venta) {
    }

    /**
     * @param ventas ventas pendientes
     */
    public record SincronizacionRequest(@NotEmpty(message = "{validacion.lineas}")
                                        @Size(max = 500, message = "{validacion.longitud}")
                                        @Valid List<VentaOffline> ventas) {
    }

    /**
     * @param idLocal       UUID local
     * @param estado        APLICADA, DUPLICADA o CONFLICTO
     * @param facturaId     factura generada
     * @param numeroFactura número de factura
     * @param mensaje       detalle del conflicto
     */
    public record ResultadoSincronizacion(UUID idLocal, String estado, Long facturaId, String numeroFactura,
                                          String mensaje) {
    }

    /**
     * @param id             id
     * @param idLocal        UUID local
     * @param dispositivo    puesto
     * @param fechaLocal     fecha offline
     * @param fechaRecepcion fecha de recepción
     * @param estado         estado
     * @param mensaje        mensaje
     * @param numeroFactura  factura
     * @param payload        venta original (JSON)
     * @param usuario        usuario
     * @param resueltoPor    supervisor
     */
    public record OperacionResponse(Long id, UUID idLocal, String dispositivo, OffsetDateTime fechaLocal,
                                    OffsetDateTime fechaRecepcion, String estado, String mensaje, String numeroFactura,
                                    String payload, String usuario, String resueltoPor) {

        /**
         * @param o operación
         * @return DTO
         */
        public static OperacionResponse de(SincronizacionOperacion o) {
            return new OperacionResponse(o.getId(), o.getIdLocal(), o.getDispositivo(), o.getFechaLocal(),
                    o.getFechaRecepcion(), o.getEstado().name(), o.getMensaje(),
                    o.getFactura() == null ? null : o.getFactura().getNumero(), o.getPayload(), o.getUsuario(),
                    o.getResueltoPor());
        }
    }

    /**
     * Producto mínimo para operar sin internet.
     *
     * @param id               id
     * @param codigoBarras     código
     * @param nombre           nombre
     * @param marca            marca
     * @param unidadBaseId     unidad base
     * @param unidadBase       código unidad base
     * @param permiteFraccion  fraccionable
     * @param precioVenta      precio por unidad base
     * @param porcentajeIva    IVA
     * @param disponible       disponible al momento de la descarga
     * @param tieneGarantia    garantía
     * @param mesesGarantia    meses
     * @param conversiones     conversiones de venta
     */
    public record ProductoOffline(Long id, String codigoBarras, String nombre, String marca, Long unidadBaseId,
                                  String unidadBase, boolean permiteFraccion, BigDecimal precioVenta,
                                  BigDecimal porcentajeIva, BigDecimal disponible, boolean tieneGarantia,
                                  Integer mesesGarantia, List<ConversionOffline> conversiones) {
    }

    /**
     * @param unidadId unidad
     * @param codigo   código
     * @param factor   factor a unidad base
     */
    public record ConversionOffline(Long unidadId, String codigo, BigDecimal factor) {
    }

    /**
     * @param id              id
     * @param numeroDocumento documento
     * @param nombre          nombre
     */
    public record ClienteOffline(Long id, String numeroDocumento, String nombre) {
    }

    /**
     * @param generadoEn fecha de generación
     * @param productos  productos activos
     * @param clientes   clientes activos
     */
    public record CatalogoOffline(OffsetDateTime generadoEn, List<ProductoOffline> productos,
                                  List<ClienteOffline> clientes) {
    }
}
