/*
 * Nombre: ProductoRepositories.java
 * Descripción: Repositorios JPA de productos, conversiones e historial de precios.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorios del módulo de productos.
 */
public final class ProductoRepositories {

    private ProductoRepositories() {
    }

    /** Repositorio de productos. */
    public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

        /**
         * @param codigoBarras código de barras
         * @return producto con sus relaciones cargadas
         */
        @EntityGraph(attributePaths = {"categoria", "marca", "presentacion", "unidadBase"})
        Optional<Producto> findByCodigoBarras(String codigoBarras);

        /**
         * @param id id
         * @return producto con sus relaciones cargadas
         */
        @EntityGraph(attributePaths = {"categoria", "marca", "presentacion", "unidadBase"})
        Optional<Producto> findWithRelacionesById(Long id);

        /**
         * @param codigoBarras código
         * @return true si existe
         */
        boolean existsByCodigoBarras(String codigoBarras);

        /**
         * @param codigoBarras código
         * @param id           id a excluir
         * @return true si otro producto tiene el código
         */
        boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

        /**
         * @param estado estado
         * @return productos en el estado
         */
        @EntityGraph(attributePaths = {"categoria", "marca", "presentacion", "unidadBase"})
        List<Producto> findByEstado(EstadoProducto estado);
    }

    /** Repositorio de conversiones de unidad. */
    public interface ConversionUnidadRepository extends JpaRepository<ConversionUnidad, Long> {

        /**
         * @param productoId producto
         * @return conversiones del producto
         */
        @EntityGraph(attributePaths = {"unidad"})
        List<ConversionUnidad> findByProductoIdOrderByFactorAsc(Long productoId);

        /**
         * @param productoId producto
         * @param unidadId   unidad
         * @return conversión
         */
        Optional<ConversionUnidad> findByProductoIdAndUnidadId(Long productoId, Long unidadId);

        /**
         * @param productoIds productos
         * @return conversiones
         */
        @EntityGraph(attributePaths = {"unidad", "producto"})
        List<ConversionUnidad> findByProductoIdIn(List<Long> productoIds);
    }

    /** Repositorio del historial de precios. */
    public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {

        /**
         * @param productoId producto
         * @return historial descendente por fecha
         */
        List<HistorialPrecio> findByProductoIdOrderByFechaDescIdDesc(Long productoId);
    }
}
