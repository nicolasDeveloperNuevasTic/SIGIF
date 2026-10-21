/*
 * Nombre: RolRepository.java
 * Descripción: Repositorio JPA de roles.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Rol}.
 */
public interface RolRepository extends JpaRepository<Rol, Long> {

    /**
     * @param codigo código del rol
     * @return rol si existe
     */
    Optional<Rol> findByCodigo(String codigo);

    /**
     * @param codigos códigos
     * @return roles encontrados
     */
    List<Rol> findByCodigoIn(List<String> codigos);
}
