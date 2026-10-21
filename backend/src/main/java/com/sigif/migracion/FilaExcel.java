/*
 * Nombre: FilaExcel.java
 * Descripción: Acceso tipado a las celdas de una fila de Excel por nombre de columna.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.migracion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

/**
 * Envuelve una fila de POI y su mapa de encabezados (nombre de columna → índice).
 *
 * @param row         fila
 * @param encabezados índice de cada columna
 */
public record FilaExcel(Row row, Map<String, Integer> encabezados) {

    private static final DataFormatter FORMATO = new DataFormatter();

    /**
     * @return número de fila tal como lo muestra Excel (base 1)
     */
    public int numero() {
        return row.getRowNum() + 1;
    }

    /**
     * @return true si todas las celdas están vacías
     */
    public boolean vacia() {
        return encabezados.values().stream().allMatch(i -> FORMATO.formatCellValue(row.getCell(i)).isBlank());
    }

    /**
     * @param columna     columna
     * @param obligatorio lanza error si está vacía
     * @return texto recortado o null
     */
    public String texto(String columna, boolean obligatorio) {
        Cell c = celda(columna);
        String valor;
        if (c != null && c.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(c)) {
            valor = BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString();
        } else {
            valor = c == null ? "" : FORMATO.formatCellValue(c).trim();
        }
        if (valor.isEmpty()) {
            if (obligatorio) {
                throw new IllegalArgumentException(columna + ": obligatorio / required");
            }
            return null;
        }
        return valor;
    }

    /**
     * @param columna     columna
     * @param obligatorio obligatorio
     * @return decimal o null
     */
    public BigDecimal decimal(String columna, boolean obligatorio) {
        Cell c = celda(columna);
        if (c != null && c.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(c.getNumericCellValue());
        }
        String t = texto(columna, obligatorio);
        if (t == null) {
            return null;
        }
        try {
            return new BigDecimal(t.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(columna + ": número inválido / invalid number (" + t + ")");
        }
    }

    /**
     * @param columna columna
     * @return entero o null
     */
    public Integer entero(String columna) {
        BigDecimal d = decimal(columna, false);
        return d == null ? null : d.intValueExact();
    }

    /**
     * @param columna columna
     * @return true para SI/SÍ/YES/TRUE/1/X
     */
    public boolean booleano(String columna) {
        Cell c = celda(columna);
        if (c != null && c.getCellType() == CellType.BOOLEAN) {
            return c.getBooleanCellValue();
        }
        String t = texto(columna, false);
        return t != null && switch (t.toUpperCase()) {
            case "SI", "SÍ", "S", "YES", "Y", "TRUE", "VERDADERO", "1", "X" -> true;
            default -> false;
        };
    }

    /**
     * @param columna columna
     * @return fecha (celda de fecha o texto AAAA-MM-DD) o null
     */
    public LocalDate fecha(String columna) {
        Cell c = celda(columna);
        if (c != null && c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate();
        }
        String t = texto(columna, false);
        try {
            return t == null ? null : LocalDate.parse(t);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(columna + ": fecha inválida, use AAAA-MM-DD / invalid date (" + t + ")");
        }
    }

    private Cell celda(String columna) {
        Integer i = encabezados.get(columna.toLowerCase());
        return i == null ? null : row.getCell(i);
    }
}
