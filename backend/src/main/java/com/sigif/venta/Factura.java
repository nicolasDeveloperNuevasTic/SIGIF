/*
 * Nombre: Factura.java
 * Descripción: Factura generada por cada salida por venta (RN-010).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.venta;

import com.sigif.tercero.Tercero;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Factura con cliente, productos, precios, garantías y total (REQ-F-022).
 */
@Entity
@Getter
@Setter
@Table(name = "factura")
public class Factura {

    /** Canal de origen de la venta. */
    public enum Origen {
        /** Punto de venta en línea. */
        EN_LINEA,
        /** Venta registrada sin internet y sincronizada. */
        OFFLINE,
        /** Sistema externo. */
        API
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Tercero cliente;

    private OffsetDateTime fecha;

    private Integer periodo;

    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_descuento")
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(name = "total_iva")
    private BigDecimal totalIva = BigDecimal.ZERO;

    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Origen origen = Origen.EN_LINEA;

    @Column(name = "id_local_offline")
    private UUID idLocalOffline;

    private String observaciones;

    private String usuario;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleFactura> detalles = new ArrayList<>();
}
