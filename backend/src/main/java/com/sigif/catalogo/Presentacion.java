/*
 * Nombre: Presentacion.java
 * Descripción: Entidad de presentación comercial de productos (RN-004).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Presentación (Unidad, Caja, Rollo, Galón...).
 */
@Entity
@Table(name = "presentacion")
public class Presentacion extends CatalogoBase {
}
