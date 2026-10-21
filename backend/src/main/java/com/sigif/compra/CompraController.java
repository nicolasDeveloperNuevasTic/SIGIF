/*
 * Nombre: CompraController.java
 * Descripción: Endpoints REST de entradas por compra y Documentos de Ingreso.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.compra;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.compra.CompraDtos.CompraRequest;
import com.sigif.compra.CompraDtos.DocumentoIngresoResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/v1/compras: POST registra la compra y devuelve el Documento de Ingreso.
 */
@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService service;

    /**
     * @param req compra
     * @return Documento de Ingreso (201)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COMPRA_REGISTRAR')")
    public DocumentoIngresoResponse registrar(@Valid @RequestBody CompraRequest req) {
        return service.registrar(req);
    }

    /**
     * @param proveedorId proveedor
     * @param numero      número
     * @param desde       desde
     * @param hasta       hasta
     * @param pagina      página
     * @param tamano      tamaño
     * @return documentos
     */
    @GetMapping
    @PreAuthorize("hasAuthority('COMPRA_LEER')")
    public PaginaResponse<DocumentoIngresoResponse> buscar(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return service.buscar(proveedorId, numero, desde, hasta, pagina, tamano);
    }

    /**
     * @param id id
     * @return documento con detalle
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRA_LEER')")
    public DocumentoIngresoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
