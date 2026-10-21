/*
 * Nombre: VentaController.java
 * Descripción: Endpoints REST de salidas por venta y facturas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.venta;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.venta.VentaDtos.FacturaResponse;
import com.sigif.venta.VentaDtos.VentaRequest;
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
 * /api/v1/ventas: POST registra la venta y devuelve la Factura.
 */
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService service;

    /**
     * @param req venta
     * @return factura (201)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VENTA_REGISTRAR')")
    public FacturaResponse registrar(@Valid @RequestBody VentaRequest req) {
        return service.registrar(req);
    }

    /**
     * @param clienteId cliente
     * @param numero    número
     * @param desde     desde
     * @param hasta     hasta
     * @param pagina    página
     * @param tamano    tamaño
     * @return facturas
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VENTA_LEER')")
    public PaginaResponse<FacturaResponse> buscar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return service.buscar(clienteId, numero, desde, hasta, pagina, tamano);
    }

    /**
     * @param id id
     * @return factura con detalle
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VENTA_LEER')")
    public FacturaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
