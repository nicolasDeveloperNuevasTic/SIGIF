/*
 * Nombre: CatalogoRepositories.java
 * Descripción: Repositorios JPA de los catálogos maestros (categoría, marca, presentación, unidad).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorios de los catálogos maestros. Spring Data los detecta porque la aplicación
 * habilita {@code considerNestedRepositories} en {@link com.sigif.config.JpaConfig}.
 */
public final class CatalogoRepositories {

    private CatalogoRepositories() {
    }

    /** Repositorio de categorías. */
    public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
        /**
         * @param nombre nombre
         * @return categoría raíz con ese nombre
         */
        Optional<Categoria> findFirstByNombreIgnoreCase(String nombre);
    }

    /** Repositorio de marcas. */
    public interface MarcaRepository extends JpaRepository<Marca, Long> {
        /**
         * @param nombre nombre
         * @return marca
         */
        Optional<Marca> findFirstByNombreIgnoreCase(String nombre);
    }

    /** Repositorio de presentaciones. */
    public interface PresentacionRepository extends JpaRepository<Presentacion, Long> {
        /**
         * @param nombre nombre
         * @return presentación
         */
        Optional<Presentacion> findFirstByNombreIgnoreCase(String nombre);
    }

    /** Repositorio de unidades de medida. */
    public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
        /**
         * @param codigo código
         * @return unidad
         */
        Optional<UnidadMedida> findFirstByCodigoIgnoreCase(String codigo);
    }
}
