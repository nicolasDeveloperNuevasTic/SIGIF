/*
 * Nombre: SincronizacionOperacion.java
 * Descripción: Registro idempotente de cada operación offline recibida y su resultado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.sincronizacion;

import com.sigif.venta.Factura;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

/**
 * El {@code idLocal} (UUID generado en el navegador) garantiza que reenviar la misma venta no
 * la duplique.
 */
@Entity
@Getter
@Setter
@Table(name = "sincronizacion_operacion")
public class SincronizacionOperacion {

    /** Resultado de la sincronización. */
    public enum Estado {
        /** Aplicada sin novedad. */
        APLICADA,
        /** Rechazada (ej. stock insuficiente); requiere revisión. */
        CONFLICTO,
        /** Conflicto resuelto con un reintento exitoso. */
        RESUELTA,
        /** Conflicto descartado por un supervisor. */
        DESCARTADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_local")
    private UUID idLocal;

    private String tipo = "VENTA";

    private String dispositivo;

    @Column(name = "fecha_local")
    private OffsetDateTime fechaLocal;

    @Column(name = "fecha_recepcion")
    private OffsetDateTime fechaRecepcion = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private String mensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @ColumnTransformer(read = "payload::text", write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private String payload;

    private String usuario;

    @Column(name = "resuelto_por")
    private String resueltoPor;

    @Column(name = "fecha_resolucion")
    private OffsetDateTime fechaResolucion;
}
