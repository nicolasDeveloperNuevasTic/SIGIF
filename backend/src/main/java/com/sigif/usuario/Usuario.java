/*
 * Nombre: Usuario.java
 * Descripción: Entidad de usuario del sistema con contraseña encriptada (BCrypt) y roles.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Usuario autenticable. Solo se almacena el hash BCrypt de la contraseña (REQ-F-031) y el
 * hash SHA-256 de su API Key de integración, si la tiene.
 */
@Entity
@Getter
@Setter
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    private String correo;

    private String idioma = "es";

    private boolean activo = true;

    @Column(name = "api_key_hash")
    private String apiKeyHash;

    @Column(name = "ultimo_acceso")
    private OffsetDateTime ultimoAcceso;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    /**
     * @return códigos de roles y permisos efectivos del usuario (authorities)
     */
    public Set<String> autoridades() {
        return roles.stream()
                .flatMap(r -> Stream.concat(Stream.of(r.getCodigo()), r.getPermisos().stream().map(Permiso::getCodigo)))
                .collect(Collectors.toSet());
    }
}
