/*
 * Nombre: DocumentoIngreso.java
 * Descripción: Documento de Ingreso generado por cada entrada por compra (RN-010).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.compra;

import com.sigif.tercero.Tercero;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Comprobante de entrada con proveedor, detalle, costos y responsable (REQ-F-019).
 */
@Entity
@Getter
@Setter
@Table(name = "documento_ingreso")
public class DocumentoIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Tercero proveedor;

    private OffsetDateTime fecha;

    private Integer periodo;

    @Column(name = "numero_factura_proveedor")
    private String numeroFacturaProveedor;

    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_iva")
    private BigDecimal totalIva = BigDecimal.ZERO;

    private BigDecimal total = BigDecimal.ZERO;

    private String observaciones;

    private String usuario;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleIngreso> detalles = new ArrayList<>();
}
