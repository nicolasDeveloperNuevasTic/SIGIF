/*
 * Nombre: AuditoriaController.java
 * Descripción: Endpoint REST de consulta de la bitácora de auditoría.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.auditoria;

import com.sigif.common.dto.PaginaResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta filtrada de auditoría para Auditor y Administrador (REQ-F-035).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService service;

    /**
     * @param tabla      filtro por tabla
     * @param operacion  filtro por operación
     * @param usuario    filtro por usuario
     * @param registroId filtro por id de registro
     * @param desde      fecha inicial ISO-8601
     * @param hasta      fecha final ISO-8601
     * @param pagina     página
     * @param tamano     tamaño
     * @return página de registros
     */
    @GetMapping
    @PreAuthorize("hasAuthority('AUDITORIA_LEER')")
    public PaginaResponse<AuditoriaService.AuditoriaResponse> buscar(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String operacion,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String registroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return service.buscar(tabla, operacion, usuario, registroId, desde, hasta, pagina, tamano);
    }
}
