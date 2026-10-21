/*
 * Nombre: TerceroRepository.java
 * Descripción: Repositorio JPA de terceros.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.tercero;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso a datos de {@link Tercero}.
 */
public interface TerceroRepository extends JpaRepository<Tercero, Long>, JpaSpecificationExecutor<Tercero> {

    /**
     * @param numeroDocumento cédula o NIT
     * @return tercero
     */
    Optional<Tercero> findByNumeroDocumento(String numeroDocumento);

    /**
     * @param numeroDocumento documento
     * @return true si existe
     */
    boolean existsByNumeroDocumento(String numeroDocumento);

    /**
     * @param numeroDocumento documento
     * @param id              id a excluir
     * @return true si otro tercero lo tiene
     */
    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
}
