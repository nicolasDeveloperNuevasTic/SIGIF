/*
 * Nombre: CierreMensual.java
 * Descripción: Cierre mensual de inventario (encabezado) que bloquea el periodo cerrado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Cierre de un periodo AAAAMM. Una vez insertado es inmutable (trigger).
 */
@Entity
@Getter
@Setter
@Table(name = "cierre_mensual")
public class CierreMensual {

    /** Forma de ejecución. */
    public enum TipoEjecucion {
        /** Ejecutado por el programador automático. */
        PROGRAMADO,
        /** Ejecutado por un usuario autorizado. */
        MANUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer periodo;

    @Column(name = "fecha_ejecucion")
    private OffsetDateTime fechaEjecucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ejecucion")
    private TipoEjecucion tipoEjecucion;

    @Column(name = "operaciones_pendientes")
    private int operacionesPendientes;

    @Column(name = "valor_inventario")
    private BigDecimal valorInventario = BigDecimal.ZERO;

    private String observaciones;

    private String usuario;

    @OneToMany(mappedBy = "cierre", cascade = CascadeType.PERSIST)
    private List<CierreMensualDetalle> detalles = new ArrayList<>();
}
