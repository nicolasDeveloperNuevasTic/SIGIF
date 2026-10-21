/*
 * Nombre: CierreController.java
 * Descripción: Endpoints REST de cierres mensuales.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

import com.sigif.cierre.CierreService.CierreRequest;
import com.sigif.cierre.CierreService.CierreResponse;
import com.sigif.cierre.CierreService.PrevalidacionResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
 * /api/v1/cierres.
 */
@RestController
@RequestMapping("/api/v1/cierres")
@RequiredArgsConstructor
public class CierreController {

    private final CierreService service;

    /**
     * @return cierres
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CIERRE_LEER')")
    public List<CierreResponse> listar() {
        return service.listar();
    }

    /**
     * @param id id
     * @return cierre con detalle
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CIERRE_LEER')")
    public CierreResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /**
     * @param periodo AAAAMM
     * @return alertas previas
     */
    @GetMapping("/prevalidacion")
    @PreAuthorize("hasAuthority('CIERRE_LEER')")
    public PrevalidacionResponse prevalidar(@RequestParam int periodo) {
        return service.prevalidar(periodo);
    }

    /**
     * @param req periodo y forzar
     * @return cierre ejecutado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CIERRE_EJECUTAR')")
    public CierreResponse ejecutar(@Valid @RequestBody CierreRequest req) {
        return service.ejecutar(req.periodo(), CierreMensual.TipoEjecucion.MANUAL, req.forzar());
    }
}
