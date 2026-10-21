/*
 * Nombre: SincronizacionController.java
 * Descripción: Endpoints REST del modo offline: catálogo local y sincronización de ventas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.sincronizacion;

import com.sigif.sincronizacion.SincronizacionDtos.CatalogoOffline;
import com.sigif.sincronizacion.SincronizacionDtos.OperacionResponse;
import com.sigif.sincronizacion.SincronizacionDtos.ResultadoSincronizacion;
import com.sigif.sincronizacion.SincronizacionDtos.SincronizacionRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/v1/sincronizacion.
 */
@RestController
@RequestMapping("/api/v1/sincronizacion")
@RequiredArgsConstructor
public class SincronizacionController {

    private final SincronizacionService service;

    /**
     * @return catálogo para la caché IndexedDB
     */
    @GetMapping("/catalogo")
    @PreAuthorize("hasAuthority('PRODUCTO_LEER')")
    public CatalogoOffline catalogo() {
        return service.catalogo();
    }

    /**
     * @param req ventas registradas sin conexión
     * @return resultado por venta (idempotente por idLocal)
     */
    @PostMapping("/ventas")
    @PreAuthorize("hasAuthority('SINCRONIZACION_EJECUTAR') and hasAuthority('VENTA_REGISTRAR')")
    public List<ResultadoSincronizacion> sincronizar(@Valid @RequestBody SincronizacionRequest req) {
        return service.sincronizar(req);
    }

    /**
     * @param estado filtro
     * @return operaciones
     */
    @GetMapping("/operaciones")
    @PreAuthorize("hasAuthority('SINCRONIZACION_EJECUTAR') or hasAuthority('SINCRONIZACION_RESOLVER')")
    public List<OperacionResponse> operaciones(@RequestParam(required = false) SincronizacionOperacion.Estado estado) {
        return service.operaciones(estado);
    }

    /**
     * @param id operación
     * @return operación resuelta
     */
    @PostMapping("/operaciones/{id}/reintentar")
    @PreAuthorize("hasAuthority('SINCRONIZACION_RESOLVER')")
    public OperacionResponse reintentar(@PathVariable Long id) {
        return service.reintentar(id);
    }

    /**
     * @param id operación
     * @return operación descartada
     */
    @PostMapping("/operaciones/{id}/descartar")
    @PreAuthorize("hasAuthority('SINCRONIZACION_RESOLVER')")
    public OperacionResponse descartar(@PathVariable Long id) {
        return service.descartar(id);
    }
}
