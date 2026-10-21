/*
 * Nombre: Tercero.java
 * Descripción: Entidad de tercero (cliente y/o proveedor), persona natural o jurídica.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.tercero;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Tercero con los datos de RN-012: cédula o NIT, nombre, apellido, razón social, dirección,
 * teléfono y correo.
 */
@Entity
@Getter
@Setter
@Table(name = "tercero")
public class Tercero {

    /** Tipo de persona. */
    public enum TipoPersona {
        /** Persona natural (cédula). */
        NATURAL,
        /** Persona jurídica (NIT). */
        JURIDICA
    }

    /** Tipo de documento de identificación. */
    public enum TipoDocumento {
        /** Cédula de ciudadanía. */
        CC,
        /** Cédula de extranjería. */
        CE,
        /** Número de identificación tributaria. */
        NIT,
        /** Pasaporte. */
        PAS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona")
    private TipoPersona tipoPersona;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "digito_verificacion")
    private String digitoVerificacion;

    private String nombre;

    private String apellido;

    @Column(name = "razon_social")
    private String razonSocial;

    private String direccion;

    private String telefono;

    private String correo;

    @Column(name = "es_cliente")
    private boolean esCliente;

    @Column(name = "es_proveedor")
    private boolean esProveedor;

    @Column(name = "es_generico")
    private boolean esGenerico;

    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    /**
     * @return razón social o "nombre apellido"
     */
    public String nombreMostrar() {
        if (razonSocial != null && !razonSocial.isBlank()) {
            return razonSocial;
        }
        return (nombre + " " + apellido).trim();
    }
}
