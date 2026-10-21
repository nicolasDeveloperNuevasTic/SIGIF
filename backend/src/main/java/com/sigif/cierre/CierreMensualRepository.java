/*
 * Nombre: CierreMensualRepository.java
 * Descripción: Repositorio JPA de cierres mensuales.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Acceso a datos de {@link CierreMensual}.
 */
public interface CierreMensualRepository extends JpaRepository<CierreMensual, Long> {

    /**
     * @param periodo AAAAMM
     * @return true si el periodo está cerrado
     */
    boolean existsByPeriodo(Integer periodo);

    /**
     * @return cierres del más reciente al más antiguo
     */
    List<CierreMensual> findAllByOrderByPeriodoDesc();

    /**
     * @param periodo periodo
     * @return cierre
     */
    Optional<CierreMensual> findByPeriodo(Integer periodo);

    /**
     * @return último periodo cerrado
     */
    @Query("select max(c.periodo) from CierreMensual c")
    Optional<Integer> ultimoPeriodoCerrado();

    /**
     * @param cierreId cierre
     * @return detalles con producto
     */
    @Query("select d from CierreMensualDetalle d join fetch d.producto p where d.cierre.id = :cierreId order by p.nombre")
    List<CierreMensualDetalle> detalles(Long cierreId);
}
