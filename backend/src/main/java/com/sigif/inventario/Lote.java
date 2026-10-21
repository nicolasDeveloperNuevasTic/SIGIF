/*
 * Nombre: Lote.java
 * Descripción: Lote de un producto con fecha de vencimiento (control por lotes, FEFO).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.compra.DocumentoIngreso;
import com.sigif.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Cada compra de un producto con vencimiento crea (o incrementa) un lote; las ventas consumen
 * primero el lote vigente que vence antes (First-Expired, First-Out).
 */
@Entity
@Getter
@Setter
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "codigo_lote")
    private String codigoLote;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "cantidad_inicial")
    private BigDecimal cantidadInicial;

    @Column(name = "cantidad_actual")
    private BigDecimal cantidadActual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_ingreso_id")
    private DocumentoIngreso documentoIngreso;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;
}
