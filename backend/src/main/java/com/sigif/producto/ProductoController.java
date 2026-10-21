/*
 * Nombre: ProductoController.java
 * Descripción: Contrato REST JSON de la entidad Producto (/api/v1/productos).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.producto.ConversionService.ConversionRequest;
import com.sigif.producto.ConversionService.ConversionResponse;
import com.sigif.producto.ProductoDtos.EstadoRequest;
import com.sigif.producto.ProductoDtos.HistorialPrecioResponse;
import com.sigif.producto.ProductoDtos.PrecioRequest;
import com.sigif.producto.ProductoDtos.ProductoRequest;
import com.sigif.producto.ProductoDtos.ProductoResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints de la sección 9.3: POST, GET (lista/id/código de barras), PUT, PATCH estado y DELETE.
 */
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;
    private final ConversionService conversionService;

    /**
     * @param req datos del producto
     * @return 201 Created con ProductoResponse
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_ESCRIBIR')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = service.crear(req);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.id())).body(creado);
    }

    /**
     * @param nombre      filtro por nombre o código
     * @param categoriaId filtro por categoría
     * @param marcaId     filtro por marca
     * @param estado      filtro por estado
     * @param pagina      página (base 0)
     * @param tamano      tamaño
     * @param orden       campo de orden
     * @return página de productos
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_LEER')")
    public PaginaResponse<ProductoResponse> listar(@RequestParam(required = false) String nombre,
                                                   @RequestParam(required = false) Long categoriaId,
                                                   @RequestParam(required = false) Long marcaId,
                                                   @RequestParam(required = false) EstadoProducto estado,
                                                   @RequestParam(defaultValue = "0") int pagina,
                                                   @RequestParam(defaultValue = "20") int tamano,
                                                   @RequestParam(defaultValue = "nombre") String orden) {
        return service.listar(nombre, categoriaId, marcaId, estado, pagina, tamano, orden);
    }

    /**
     * @param id id
     * @return producto o 404
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_LEER')")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /**
     * @param codigo código de barras
     * @return producto o 404
     */
    @GetMapping("/codigo-barras/{codigo}")
    @PreAuthorize("hasAuthority('PRODUCTO_LEER')")
    public ProductoResponse porCodigo(@PathVariable String codigo) {
        return service.porCodigoBarras(codigo);
    }

    /**
     * @param id  id
     * @param req datos
     * @return producto actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_ESCRIBIR')")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    /**
     * @param id  id
     * @param req estado de catálogo (ACTIVO/INACTIVO)
     * @return producto
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PRODUCTO_ESCRIBIR')")
    public ProductoResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {
        return service.cambiarEstado(id, req.estado());
    }

    /**
     * @param id id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCTO_ESCRIBIR')")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    /**
     * @param id  id
     * @param req costo y porcentaje
     * @return producto con precio recalculado
     */
    @PatchMapping("/{id}/precio")
    @PreAuthorize("hasAuthority('PRECIO_ESCRIBIR')")
    public ProductoResponse precio(@PathVariable Long id, @Valid @RequestBody PrecioRequest req) {
        return service.actualizarPrecio(id, req.precioCompra(), req.porcentajeIncremento());
    }

    /**
     * @param id id
     * @return historial de costos y precios
     */
    @GetMapping("/{id}/historial-precios")
    @PreAuthorize("hasAuthority('PRECIO_HISTORIAL_LEER')")
    public List<HistorialPrecioResponse> historial(@PathVariable Long id) {
        return service.historial(id);
    }

    /**
     * @param id id
     * @return conversiones del producto
     */
    @GetMapping("/{id}/conversiones")
    @PreAuthorize("hasAuthority('PRODUCTO_LEER')")
    public List<ConversionResponse> conversiones(@PathVariable Long id) {
        return conversionService.listar(id);
    }

    /**
     * @param id  id
     * @param req conversión
     * @return conversión creada
     */
    @PostMapping("/{id}/conversiones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CATALOGO_ESCRIBIR')")
    public ConversionResponse agregarConversion(@PathVariable Long id, @Valid @RequestBody ConversionRequest req) {
        return conversionService.agregar(id, req);
    }

    /**
     * @param id           producto
     * @param conversionId conversión
     */
    @DeleteMapping("/{id}/conversiones/{conversionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CATALOGO_ESCRIBIR')")
    public void eliminarConversion(@PathVariable Long id, @PathVariable Long conversionId) {
        conversionService.eliminar(id, conversionId);
    }
}
