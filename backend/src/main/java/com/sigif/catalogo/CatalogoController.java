/*
 * Nombre: CatalogoController.java
 * Descripción: Endpoints REST de catálogos maestros (categorías, marcas, presentaciones, unidades).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import com.sigif.catalogo.CatalogoService.CatalogoRequest;
import com.sigif.catalogo.CatalogoService.CatalogoResponse;
import com.sigif.catalogo.CatalogoService.TipoCatalogo;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas: /api/v1/catalogos/{categorias|marcas|presentaciones|unidades-medida}.
 */
@RestController
@RequestMapping("/api/v1/catalogos/{tipo}")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService service;

    /**
     * @param tipo tipo de catálogo
     * @return elementos
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_LEER')")
    public List<CatalogoResponse> listar(@PathVariable String tipo) {
        return service.listar(TipoCatalogo.deRuta(tipo));
    }

    /**
     * @param tipo tipo
     * @param req  datos
     * @return creado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CATALOGO_ESCRIBIR')")
    public CatalogoResponse crear(@PathVariable String tipo, @Valid @RequestBody CatalogoRequest req) {
        return service.crear(TipoCatalogo.deRuta(tipo), req);
    }

    /**
     * @param tipo tipo
     * @param id   id
     * @param req  datos
     * @return actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOGO_ESCRIBIR')")
    public CatalogoResponse actualizar(@PathVariable String tipo, @PathVariable Long id,
                                       @Valid @RequestBody CatalogoRequest req) {
        return service.actualizar(TipoCatalogo.deRuta(tipo), id, req);
    }
}
