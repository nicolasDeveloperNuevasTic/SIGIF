/*
 * Nombre: DetalleFactura.java
 * Descripción: Línea de producto vendida en una factura, con garantía aplicable.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.venta;

import com.sigif.catalogo.UnidadMedida;
import com.sigif.inventario.Reserva;
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
import lombok.Getter;
import lombok.Setter;

/**
 * Cantidad vendida (entera o fracción) en la unidad del documento y su equivalente base.
 */
@Entity
@Getter
@Setter
@Table(name = "detalle_factura")
public class DetalleFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id")
    private Factura factura;

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

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "porcentaje_descuento")
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "porcentaje_iva")
    private BigDecimal porcentajeIva = BigDecimal.ZERO;

    private BigDecimal subtotal;

    @Column(name = "valor_descuento")
    private BigDecimal valorDescuento = BigDecimal.ZERO;

    @Column(name = "valor_iva")
    private BigDecimal valorIva = BigDecimal.ZERO;

    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(name = "meses_garantia")
    private Integer mesesGarantia;

    @Column(name = "fecha_fin_garantia")
    private LocalDate fechaFinGarantia;

    @Column(name = "numero_serie")
    private String numeroSerie;
}
