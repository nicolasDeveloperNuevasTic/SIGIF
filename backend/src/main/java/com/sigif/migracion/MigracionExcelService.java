/*
 * Nombre: MigracionExcelService.java
 * Descripción: Lectura del libro Excel de migración, importación por hojas e informe de filas rechazadas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.migracion;

import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.excepcion.SigifException;
import com.sigif.common.util.Mensajes;
import com.sigif.migracion.MigracionDtos.ErrorFila;
import com.sigif.migracion.MigracionDtos.ResultadoMigracion;
import com.sigif.migracion.MigracionDtos.ResumenHoja;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Estructura del libro (pregunta 11.4 #15): hojas {@code Productos}, {@code Terceros} e
 * {@code InventarioInicial}, en ese orden, con encabezados en la fila 1. Las hojas ausentes se
 * omiten. Se importan los registros válidos y se informa fila y causa de cada rechazo.
 */
@Service
@RequiredArgsConstructor
public class MigracionExcelService {

    /** Columnas por hoja, en el orden de la plantilla. */
    public static final Map<String, List<String>> COLUMNAS = new LinkedHashMap<>();

    static {
        COLUMNAS.put("Productos", List.of("codigoBarras", "nombre", "categoria", "marca", "presentacion", "unidadBase",
                "descripcion", "permiteFraccion", "precioCompra", "porcentajeIncremento", "porcentajeIva",
                "tieneGarantia", "mesesGarantia", "tieneVencimiento", "stockMinimo"));
        COLUMNAS.put("Terceros", List.of("tipoPersona", "tipoDocumento", "numeroDocumento", "digitoVerificacion",
                "nombre", "apellido", "razonSocial", "direccion", "telefono", "correo", "esCliente", "esProveedor"));
        COLUMNAS.put("InventarioInicial", List.of("codigoBarras", "cantidad", "costoUnitario", "codigoLote",
                "fechaVencimiento"));
    }

    private final MigracionFilaService filaService;
    private final Mensajes mensajes;

    /**
     * @param nombreArchivo nombre original
     * @param entrada       contenido .xlsx
     * @return informe de importación
     */
    public ResultadoMigracion importar(String nombreArchivo, InputStream entrada) {
        if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".xlsx")) {
            throw new ReglaNegocioException("migracion.formato");
        }
        List<ResumenHoja> hojas = new ArrayList<>();
        List<ErrorFila> errores = new ArrayList<>();
        try (Workbook libro = new XSSFWorkbook(entrada)) {
            procesarHoja(libro, "Productos", filaService::producto, hojas, errores);
            procesarHoja(libro, "Terceros", filaService::tercero, hojas, errores);
            procesarHoja(libro, "InventarioInicial", filaService::inventario, hojas, errores);
        } catch (IOException | org.apache.poi.EmptyFileException | org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException ex) {
            throw new ReglaNegocioException("migracion.formato");
        }
        if (hojas.isEmpty()) {
            throw new ReglaNegocioException("migracion.sin.hojas");
        }
        return new ResultadoMigracion(nombreArchivo, hojas, errores);
    }

    /**
     * @return plantilla .xlsx con las hojas y encabezados esperados
     * @throws IOException error de escritura
     */
    public byte[] plantilla() throws IOException {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle estilo = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            estilo.setFont(fuente);
            COLUMNAS.forEach((hoja, columnas) -> {
                Sheet s = libro.createSheet(hoja);
                Row r = s.createRow(0);
                for (int i = 0; i < columnas.size(); i++) {
                    Cell c = r.createCell(i);
                    c.setCellValue(columnas.get(i));
                    c.setCellStyle(estilo);
                    s.setColumnWidth(i, 20 * 256);
                }
            });
            Row ejemplo = libro.getSheet("Productos").createRow(1);
            Object[] valores = {"7701234567890", "Taladro Percutor 1/2 850W", "Herramientas Eléctricas", "DeWalt",
                "Unidad", "UND", "Velocidad variable", "NO", 250000, 30, 19, "SI", 12, "NO", 2};
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] instanceof Number n) {
                    ejemplo.createCell(i).setCellValue(n.doubleValue());
                } else {
                    ejemplo.createCell(i).setCellValue(String.valueOf(valores[i]));
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private void procesarHoja(Workbook libro, String nombre, Consumer<FilaExcel> importador,
                              List<ResumenHoja> hojas, List<ErrorFila> errores) {
        Sheet hoja = libro.getSheet(nombre);
        if (hoja == null) {
            return;
        }
        Row cabecera = hoja.getRow(0);
        if (cabecera == null) {
            errores.add(new ErrorFila(nombre, 1, mensajes.obtener("migracion.sin.encabezados")));
            hojas.add(new ResumenHoja(nombre, 0, 0, 0));
            return;
        }
        Map<String, Integer> indices = new HashMap<>();
        for (Cell c : cabecera) {
            indices.put(c.getStringCellValue().trim().toLowerCase(), c.getColumnIndex());
        }
        List<String> faltantes = COLUMNAS.get(nombre).stream().filter(c -> !indices.containsKey(c.toLowerCase())).toList();
        if (!faltantes.isEmpty()) {
            errores.add(new ErrorFila(nombre, 1, mensajes.obtener("migracion.columnas.faltantes", String.join(", ", faltantes))));
            hojas.add(new ResumenHoja(nombre, 0, 0, 0));
            return;
        }
        int leidas = 0;
        int importadas = 0;
        for (int i = 1; i <= hoja.getLastRowNum(); i++) {
            Row row = hoja.getRow(i);
            if (row == null) {
                continue;
            }
            FilaExcel fila = new FilaExcel(row, indices);
            if (fila.vacia()) {
                continue;
            }
            leidas++;
            try {
                importador.accept(fila);
                importadas++;
            } catch (SigifException ex) {
                errores.add(new ErrorFila(nombre, fila.numero(), mensajes.obtener(ex.getClave(), ex.getArgumentos())));
            } catch (DataAccessException ex) {
                errores.add(new ErrorFila(nombre, fila.numero(), mensajes.obtener("error.integridad")));
            } catch (RuntimeException ex) {
                errores.add(new ErrorFila(nombre, fila.numero(), ex.getMessage()));
            }
        }
        hojas.add(new ResumenHoja(nombre, leidas, importadas, leidas - importadas));
    }
}
