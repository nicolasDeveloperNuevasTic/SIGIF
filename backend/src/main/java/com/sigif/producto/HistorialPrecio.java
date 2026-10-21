/*
 * Nombre: HistorialPrecio.java
 * Descripción: Registro histórico de costos, porcentajes de incremento y precios de venta.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

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

/**
 * Historial cronológico de precios (REQ-F-010).
 */
@Entity
@Getter
@Setter
@Table(name = "historial_precio")
public class HistorialPrecio {

    /** Motivo del cambio de precio. */
    public enum Motivo {
        /** Alta del producto. */
        CREACION,
        /** Cambio manual de costo o porcentaje. */
        ACTUALIZACION_MANUAL,
        /** Recalculo por costo promedio tras una compra. */
        COMPRA,
        /** Carga desde Excel. */
        MIGRACION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "precio_compra_anterior")
    private BigDecimal precioCompraAnterior;

    @Column(name = "precio_compra_nuevo")
    private BigDecimal precioCompraNuevo;

    @Column(name = "porcentaje_anterior")
    private BigDecimal porcentajeAnterior;

    @Column(name = "porcentaje_nuevo")
    private BigDecimal porcentajeNuevo;

    @Column(name = "precio_venta_anterior")
    private BigDecimal precioVentaAnterior;

    @Column(name = "precio_venta_nuevo")
    private BigDecimal precioVentaNuevo;

    @Enumerated(EnumType.STRING)
    private Motivo motivo;

    private String referencia;

    private String usuario;

    private OffsetDateTime fecha = OffsetDateTime.now();
}
