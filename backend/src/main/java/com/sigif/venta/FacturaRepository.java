/*
 * Nombre: FacturaRepository.java
 * Descripción: Repositorio JPA de facturas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.venta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Acceso a datos de {@link Factura}.
 */
public interface FacturaRepository extends JpaRepository<Factura, Long>, JpaSpecificationExecutor<Factura> {

    /**
     * @param id id
     * @return factura con cliente y detalle
     */
    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.producto", "detalles.unidad", "detalles.reserva"})
    Optional<Factura> findConDetalleById(Long id);

    /**
     * @param idLocal UUID generado en el cliente offline
     * @return factura si ya fue sincronizada
     */
    Optional<Factura> findByIdLocalOffline(UUID idLocal);

    /**
     * @param desde inicio
     * @param hasta fin
     * @return total vendido en el rango
     */
    @Query("select coalesce(sum(f.total), 0) from Factura f where f.fecha >= :desde and f.fecha < :hasta")
    BigDecimal totalVendido(OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * @param desde inicio
     * @param hasta fin
     * @return número de facturas en el rango
     */
    @Query("select count(f) from Factura f where f.fecha >= :desde and f.fecha < :hasta")
    long cantidad(OffsetDateTime desde, OffsetDateTime hasta);
}
