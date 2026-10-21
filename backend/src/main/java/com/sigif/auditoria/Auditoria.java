/*
 * Nombre: Auditoria.java
 * Descripción: Entidad de solo lectura de la bitácora de auditoría poblada por triggers.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Registro de auditoría: tabla, operación, usuario, fecha/hora y valores anterior/nuevo.
 */
@Entity
@Immutable
@Getter
@Table(name = "auditoria")
public class Auditoria {

    @Id
    private Long id;

    private String tabla;

    private String operacion;

    @Column(name = "registro_id")
    private String registroId;

    private String usuario;

    private OffsetDateTime fecha;

    @ColumnTransformer(read = "datos_anteriores::text")
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    private String datosAnteriores;

    @ColumnTransformer(read = "datos_nuevos::text")
    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    private String datosNuevos;

    private String detalle;
}
