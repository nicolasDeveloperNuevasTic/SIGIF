/*
 * Nombre: EstadoProducto.java
 * Descripción: Estado del producto en el catálogo (independiente de los estados de inventario).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

/**
 * Estado de catálogo. Las existencias por estado (disponible/reservada/dañada) se llevan
 * en {@code inventario_saldo}.
 */
public enum EstadoProducto {
    /** Producto vigente y comercializable. */
    ACTIVO,
    /** Producto eliminado lógicamente. */
    INACTIVO
}
