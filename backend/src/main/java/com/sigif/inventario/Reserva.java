/*
 * Nombre: Reserva.java
 * Descripción: Reserva de unidades para un cliente con fecha de expiración automática.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.producto.Producto;
import com.sigif.tercero.Tercero;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Las unidades reservadas pasan de DISPONIBLE a RESERVADA y solo pueden venderse al cliente
 * de la reserva. Al expirar vuelven a DISPONIBLE.
 */
@Entity
@Getter
@Setter
@Table(name = "reserva")
public class Reserva {

    /** Estados de la reserva. */
    public enum Estado {
        /** Vigente. */
        ACTIVA,
        /** Vendida por completo. */
        CONSUMIDA,
        /** Vencida y liberada. */
        EXPIRADA,
        /** Cancelada manualmente. */
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Tercero cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private BigDecimal cantidad;

    @Column(name = "cantidad_pendiente")
    private BigDecimal cantidadPendiente;

    @Column(name = "fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.ACTIVA;

    private String observaciones;

    private String usuario;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;
}
