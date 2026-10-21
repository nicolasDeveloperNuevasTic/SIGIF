/*
 * Nombre: MigracionController.java
 * Descripción: Endpoints de carga del Excel de migración y descarga de la plantilla.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.migracion;

import com.sigif.migracion.MigracionDtos.ResultadoMigracion;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * /api/v1/migracion.
 */
@RestController
@RequestMapping("/api/v1/migracion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MIGRACION_EJECUTAR')")
public class MigracionController {

    private final MigracionExcelService service;

    /**
     * @param archivo libro .xlsx
     * @return informe de importación
     * @throws IOException error de lectura
     */
    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoMigracion importar(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return service.importar(archivo.getOriginalFilename(), archivo.getInputStream());
    }

    /**
     * @return plantilla .xlsx
     * @throws IOException error de escritura
     */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla() throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-migracion-sigif.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.plantilla());
    }
}
