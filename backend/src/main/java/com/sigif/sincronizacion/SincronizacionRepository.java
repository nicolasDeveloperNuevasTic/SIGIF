/*
 * Nombre: SincronizacionRepository.java
 * Descripción: Repositorio JPA de operaciones de sincronización offline.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.sincronizacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link SincronizacionOperacion}.
 */
public interface SincronizacionRepository extends JpaRepository<SincronizacionOperacion, Long> {

    /**
     * @param idLocal UUID local
     * @return operación ya recibida
     */
    @EntityGraph(attributePaths = {"factura"})
    Optional<SincronizacionOperacion> findByIdLocal(UUID idLocal);

    /**
     * @param estado estado
     * @return operaciones en el estado, más recientes primero
     */
    @EntityGraph(attributePaths = {"factura"})
    List<SincronizacionOperacion> findTop200ByEstadoOrderByIdDesc(SincronizacionOperacion.Estado estado);

    /**
     * @return últimas operaciones
     */
    @EntityGraph(attributePaths = {"factura"})
    List<SincronizacionOperacion> findTop200ByOrderByIdDesc();
}
