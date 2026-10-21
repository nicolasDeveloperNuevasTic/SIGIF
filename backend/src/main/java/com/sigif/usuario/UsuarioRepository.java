/*
 * Nombre: UsuarioRepository.java
 * Descripción: Repositorio JPA de usuarios.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Usuario}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * @param username nombre de usuario
     * @return usuario si existe
     */
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    /**
     * @param hash hash SHA-256 de la API Key
     * @return usuario de integración si existe
     */
    Optional<Usuario> findByApiKeyHash(String hash);

    /**
     * @param username nombre de usuario
     * @return true si existe
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * @param correo correo electrónico
     * @return true si existe
     */
    boolean existsByCorreoIgnoreCase(String correo);
}
