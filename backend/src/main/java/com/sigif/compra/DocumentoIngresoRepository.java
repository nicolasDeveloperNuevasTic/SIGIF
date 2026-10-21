/*
 * Nombre: DocumentoIngresoRepository.java
 * Descripción: Repositorio JPA de Documentos de Ingreso.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.compra;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso a datos de {@link DocumentoIngreso}.
 */
public interface DocumentoIngresoRepository extends JpaRepository<DocumentoIngreso, Long>,
        JpaSpecificationExecutor<DocumentoIngreso> {

    /**
     * @param id id
     * @return documento con proveedor y detalle
     */
    @EntityGraph(attributePaths = {"proveedor", "detalles", "detalles.producto", "detalles.unidad", "detalles.lote"})
    Optional<DocumentoIngreso> findConDetalleById(Long id);
}
