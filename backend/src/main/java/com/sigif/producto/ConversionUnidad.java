/*
 * Nombre: ConversionUnidad.java
 * Descripción: Factor de conversión entre una unidad alterna y la unidad base del producto.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import com.sigif.catalogo.UnidadMedida;
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
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 1 {@code unidad} = {@code factor} unidades base. Ej.: producto base METRO, ROLLO factor 100.
 */
@Entity
@Getter
@Setter
@Table(name = "conversion_unidad")
public class ConversionUnidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_medida_id")
    private UnidadMedida unidad;

    private BigDecimal factor;

    @Column(name = "uso_compra")
    private boolean usoCompra = true;

    @Column(name = "uso_venta")
    private boolean usoVenta = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;
}
