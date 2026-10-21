/*
 * Nombre: CierreMensualDetalle.java
 * Descripción: Consolidado por producto del cierre mensual: saldos iniciales, compras, ventas y finales por estado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

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
import lombok.Getter;
import lombok.Setter;

/**
 * Línea de consolidación (REQ-F-028).
 */
@Entity
@Getter
@Setter
@Table(name = "cierre_mensual_detalle")
public class CierreMensualDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cierre_id")
    private CierreMensual cierre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "inicial_disponible")
    private BigDecimal inicialDisponible;

    @Column(name = "inicial_reservada")
    private BigDecimal inicialReservada;

    @Column(name = "inicial_danada")
    private BigDecimal inicialDanada;

    private BigDecimal compras;

    private BigDecimal ventas;

    @Column(name = "final_disponible")
    private BigDecimal finalDisponible;

    @Column(name = "final_reservada")
    private BigDecimal finalReservada;

    @Column(name = "final_danada")
    private BigDecimal finalDanada;

    @Column(name = "costo_promedio")
    private BigDecimal costoPromedio;

    @Column(name = "valor_inventario")
    private BigDecimal valorInventario;
}
