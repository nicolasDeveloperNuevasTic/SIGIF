/*
 * Nombre: PermisoRepository.java
 * Descripción: Repositorio JPA de permisos.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Permiso}.
 */
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    /**
     * @param codigos códigos
     * @return permisos encontrados
     */
    List<Permiso> findByCodigoIn(List<String> codigos);
}
