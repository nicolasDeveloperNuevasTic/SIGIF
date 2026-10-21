/*
 * Nombre: EstadoInventario.java
 * Descripción: Estados permitidos de las existencias en la bodega única (RN-006).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

/**
 * Estados de inventario.
 */
public enum EstadoInventario {
    /** Vendible. */
    DISPONIBLE,
    /** Apartada para un cliente (vendible solo a ese cliente). */
    RESERVADA,
    /** No vendible. */
    DANADA
}
