/*
 * Nombre: TerceroController.java
 * Descripción: Endpoints REST de clientes y proveedores.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.tercero;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.tercero.TerceroDtos.TerceroRequest;
import com.sigif.tercero.TerceroDtos.TerceroResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de terceros: /api/v1/terceros.
 */
@RestController
@RequestMapping("/api/v1/terceros")
@RequiredArgsConstructor
public class TerceroController {

    private final TerceroService service;

    /**
     * @param rol    CLIENTE o PROVEEDOR
     * @param q      texto de búsqueda
     * @param pagina página
     * @param tamano tamaño
     * @return página
     */
    @GetMapping
    @PreAuthorize("hasAuthority('TERCERO_LEER')")
    public PaginaResponse<TerceroResponse> buscar(@RequestParam(required = false) TerceroService.Rol rol,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamano) {
        return service.buscar(rol, q, pagina, tamano);
    }

    /**
     * @param id id
     * @return tercero
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TERCERO_LEER')")
    public TerceroResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /**
     * @param req datos
     * @return creado (201)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TERCERO_ESCRIBIR')")
    public TerceroResponse crear(@Valid @RequestBody TerceroRequest req) {
        return service.crear(req);
    }

    /**
     * @param id  id
     * @param req datos
     * @return actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TERCERO_ESCRIBIR')")
    public TerceroResponse actualizar(@PathVariable Long id, @Valid @RequestBody TerceroRequest req) {
        return service.actualizar(id, req);
    }

    /**
     * @param id  id
     * @param req {"activo": true|false}
     * @return tercero
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('TERCERO_ESCRIBIR')")
    public TerceroResponse estado(@PathVariable Long id, @RequestBody Map<String, Boolean> req) {
        return service.cambiarEstado(id, Boolean.TRUE.equals(req.get("activo")));
    }
}
