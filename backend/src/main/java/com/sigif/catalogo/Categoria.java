/*
 * Nombre: Categoria.java
 * Descripción: Entidad de categoría de productos con jerarquía opcional (subcategorías).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Categoría. Si {@code padre} es nulo es una categoría raíz; si no, es subcategoría.
 */
@Entity
@Getter
@Setter
@Table(name = "categoria")
public class Categoria extends CatalogoBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_padre_id")
    private Categoria padre;
}
