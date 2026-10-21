/*
 * Nombre: DetalleIngreso.java
 * Descripción: Línea de producto de un Documento de Ingreso.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.compra;

import com.sigif.catalogo.UnidadMedida;
import com.sigif.inventario.Lote;
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
 * Cantidad comprada en la unidad del documento y su equivalente en unidad base.
 */
@Entity
@Getter
@Setter
@Table(name = "detalle_ingreso")
public class DetalleIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_ingreso_id")
    private DocumentoIngreso documento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_medida_id")
    private UnidadMedida unidad;

    private BigDecimal cantidad;

    @Column(name = "factor_conversion")
    private BigDecimal factorConversion;

    @Column(name = "cantidad_base")
    private BigDecimal cantidadBase;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @Column(name = "porcentaje_iva")
    private BigDecimal porcentajeIva;

    private BigDecimal subtotal;

    @Column(name = "valor_iva")
    private BigDecimal valorIva;

    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;
}
