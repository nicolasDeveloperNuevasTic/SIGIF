/*
 * Nombre: Producto.java
 * Descripción: Entidad Producto (id, nombre, categoria, descripcion, estado) extendida con marca,
 *              presentación, código de barras único, fracciones, precios, garantía y vencimiento.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import com.sigif.catalogo.Categoria;
import com.sigif.catalogo.Marca;
import com.sigif.catalogo.Presentacion;
import com.sigif.catalogo.UnidadMedida;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Cada registro representa la combinación Producto + Marca + Presentación identificada por
 * un único código de barras (RN-003/RN-004). El inventario se lleva en {@code unidadBase}.
 */
@Entity
@Getter
@Setter
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barras")
    private String codigoBarras;

    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marca_id")
    private Marca marca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentacion_id")
    private Presentacion presentacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_base_id")
    private UnidadMedida unidadBase;

    @Enumerated(EnumType.STRING)
    private EstadoProducto estado = EstadoProducto.ACTIVO;

    @Column(name = "permite_fraccion")
    private boolean permiteFraccion;

    @Column(name = "precio_compra")
    private BigDecimal precioCompra;

    @Column(name = "porcentaje_incremento")
    private BigDecimal porcentajeIncremento;

    @Column(name = "precio_venta")
    private BigDecimal precioVenta;

    @Column(name = "porcentaje_iva")
    private BigDecimal porcentajeIva = BigDecimal.ZERO;

    @Column(name = "tiene_garantia")
    private boolean tieneGarantia;

    @Column(name = "meses_garantia")
    private Integer mesesGarantia;

    @Column(name = "tiene_vencimiento")
    private boolean tieneVencimiento;

    @Column(name = "dias_alerta_vencimiento")
    private int diasAlertaVencimiento = 30;

    @Column(name = "stock_minimo")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    /**
     * @return true si el producto está activo y puede comercializarse
     */
    public boolean esActivo() {
        return estado == EstadoProducto.ACTIVO;
    }
}
