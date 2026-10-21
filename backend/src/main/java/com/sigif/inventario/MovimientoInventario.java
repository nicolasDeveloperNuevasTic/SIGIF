/*
 * Nombre: MovimientoInventario.java
 * Descripción: Movimiento inmutable de inventario: entrada por compra, salida por venta o cambio de estado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.compra.DocumentoIngreso;
import com.sigif.producto.Producto;
import com.sigif.venta.Factura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * Kardex de la bodega única (RN-001). El trigger {@code trg_movimiento_inmutable} impide su
 * modificación y {@code trg_periodo_movimiento} su registro en periodos cerrados.
 */
@Entity
@Immutable
@Getter
@Setter
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    /** Tipos de movimiento permitidos. */
    public enum Tipo {
        /** Entrada por compra (Documento de Ingreso). */
        ENTRADA_COMPRA,
        /** Salida por venta (Factura). */
        SALIDA_VENTA,
        /** Transición interna entre estados. */
        CAMBIO_ESTADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_origen")
    private EstadoInventario estadoOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_destino")
    private EstadoInventario estadoDestino;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_ingreso_id")
    private DocumentoIngreso documentoIngreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    private String motivo;

    private String usuario;

    private OffsetDateTime fecha;

    private Integer periodo;
}
