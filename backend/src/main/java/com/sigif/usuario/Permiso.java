/*
 * Nombre: Permiso.java
 * Descripción: Entidad de permiso granular del modelo RBAC.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Permiso que controla el acceso a un endpoint o pantalla (ej. PRODUCTO_ESCRIBIR).
 */
@Entity
@Getter
@Setter
@Table(name = "permiso")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;

    private String modulo;

    private String descripcion;
}
