/*
 * Nombre: MigracionExcelServiceTest.java
 * Descripción: Pruebas unitarias de la migración Excel: importa filas válidas y reporta fila y causa de las inválidas (CA-11.1).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.migracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.util.Mensajes;
import com.sigif.migracion.MigracionDtos.ResultadoMigracion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifica el aislamiento por fila y el informe de rechazos.
 */
@ExtendWith(MockitoExtension.class)
class MigracionExcelServiceTest {

    @Mock
    private MigracionFilaService filaService;
    @Mock
    private Mensajes mensajes;

    @InjectMocks
    private MigracionExcelService service;

    @Test
    void importaFilasValidasYReportaRechazadas() throws Exception {
        when(mensajes.obtener(anyString(), any(Object[].class))).thenReturn("Ya existe un producto con el código de barras 222");
        doAnswer(inv -> {
            FilaExcel f = inv.getArgument(0);
            if ("222".equals(f.texto("codigoBarras", true))) {
                throw new ConflictoException("producto.codigo.duplicado", "222");
            }
            return null;
        }).when(filaService).producto(any());

        ResultadoMigracion r = service.importar("datos.xlsx", new ByteArrayInputStream(libro(List.of("111", "222", "333"))));

        assertThat(r.hojas()).singleElement().satisfies(h -> {
            assertThat(h.hoja()).isEqualTo("Productos");
            assertThat(h.leidas()).isEqualTo(3);
            assertThat(h.importadas()).isEqualTo(2);
            assertThat(h.rechazadas()).isEqualTo(1);
        });
        assertThat(r.errores()).singleElement().satisfies(e -> {
            assertThat(e.fila()).isEqualTo(3);
            assertThat(e.causa()).contains("222");
        });
        verify(filaService, times(3)).producto(any());
    }

    @Test
    void rechazaArchivosQueNoSonXlsx() {
        assertThatThrownBy(() -> service.importar("datos.csv", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("migracion.formato");
    }

    @Test
    void generaPlantillaConLasTresHojas() throws Exception {
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(service.plantilla()))) {
            assertThat(libro.getSheet("Productos")).isNotNull();
            assertThat(libro.getSheet("Terceros")).isNotNull();
            assertThat(libro.getSheet("InventarioInicial")).isNotNull();
        }
    }

    private static byte[] libro(List<String> codigos) throws Exception {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Productos");
            Row cabecera = hoja.createRow(0);
            List<String> columnas = MigracionExcelService.COLUMNAS.get("Productos");
            for (int i = 0; i < columnas.size(); i++) {
                cabecera.createCell(i).setCellValue(columnas.get(i));
            }
            for (int i = 0; i < codigos.size(); i++) {
                Row fila = hoja.createRow(i + 1);
                fila.createCell(0).setCellValue(codigos.get(i));
                fila.createCell(1).setCellValue("Producto " + codigos.get(i));
            }
            libro.write(salida);
            return salida.toByteArray();
        }
    }
}
