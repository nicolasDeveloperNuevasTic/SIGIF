/*
 * Nombre: Marca.java
 * Descripción: Entidad de marca comercial de productos (RN-004).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Marca de producto.
 */
@Entity
@Table(name = "marca")
public class Marca extends CatalogoBase {
}
