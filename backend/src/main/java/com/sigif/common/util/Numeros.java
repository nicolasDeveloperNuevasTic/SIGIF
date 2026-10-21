/*
 * Nombre: Numeros.java
 * Descripción: Utilidades de precisión decimal para cantidades y valores monetarios.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas de redondeo del sistema: cantidades con 4 decimales, precios unitarios con 4
 * y totales monetarios con 2 (HALF_UP).
 */
public final class Numeros {

    /** Escala de cantidades de inventario. */
    public static final int ESCALA_CANTIDAD = 4;
    /** Escala de precios y costos unitarios. */
    public static final int ESCALA_PRECIO = 4;
    /** Escala de totales monetarios. */
    public static final int ESCALA_MONEDA = 2;
    /** Constante 100. */
    public static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Numeros() {
    }

    /**
     * @param valor valor a redondear
     * @return valor con 2 decimales
     */
    public static BigDecimal moneda(BigDecimal valor) {
        return valor.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
    }

    /**
     * @param valor valor a redondear
     * @return valor con 4 decimales
     */
    public static BigDecimal precio(BigDecimal valor) {
        return valor.setScale(ESCALA_PRECIO, RoundingMode.HALF_UP);
    }

    /**
     * Calcula un porcentaje de un valor.
     *
     * @param base       valor base
     * @param porcentaje porcentaje (0-100)
     * @return base * porcentaje / 100 redondeado a moneda
     */
    public static BigDecimal porcentaje(BigDecimal base, BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_MONEDA);
        }
        return moneda(base.multiply(porcentaje).divide(CIEN, 8, RoundingMode.HALF_UP));
    }

    /**
     * @param valor valor a evaluar
     * @return cantidad de decimales significativos
     */
    public static int decimalesSignificativos(BigDecimal valor) {
        return Math.max(0, valor.stripTrailingZeros().scale());
    }

    /**
     * @param valor valor a evaluar
     * @return true si no tiene parte decimal
     */
    public static boolean esEntero(BigDecimal valor) {
        return decimalesSignificativos(valor) == 0;
    }

    /**
     * @param valor valor nulo o no
     * @return el valor o cero
     */
    public static BigDecimal cero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
