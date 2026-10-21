/*
 * Nombre: ProductoService.java
 * Descripción: Lógica del CRUD de productos, código de barras único, precios y existencias.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import com.sigif.catalogo.CatalogoRepositories.CategoriaRepository;
import com.sigif.catalogo.CatalogoRepositories.MarcaRepository;
import com.sigif.catalogo.CatalogoRepositories.PresentacionRepository;
import com.sigif.catalogo.CatalogoRepositories.UnidadMedidaRepository;
import com.sigif.common.dto.PaginaResponse;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.inventario.InventarioRepositories.InventarioSaldoRepository;
import com.sigif.inventario.InventarioRepositories.LoteRepository;
import com.sigif.inventario.InventarioSaldo;
import com.sigif.inventario.InventarioService;
import com.sigif.producto.ProductoDtos.Existencias;
import com.sigif.producto.ProductoDtos.HistorialPrecioResponse;
import com.sigif.producto.ProductoDtos.ProductoRequest;
import com.sigif.producto.ProductoDtos.ProductoResponse;
import com.sigif.producto.ProductoRepositories.HistorialPrecioRepository;
import com.sigif.producto.ProductoRepositories.ProductoRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementa REQ-F-001 a REQ-F-005 y la actualización manual de precios (REQ-F-009/010).
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final PresentacionRepository presentacionRepository;
    private final UnidadMedidaRepository unidadRepository;
    private final HistorialPrecioRepository historialRepository;
    private final InventarioSaldoRepository saldoRepository;
    private final LoteRepository loteRepository;
    private final InventarioService inventarioService;
    private final PrecioService precioService;

    /**
     * @param req datos del producto
     * @return producto creado
     */
    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        return crear(req, HistorialPrecio.Motivo.CREACION);
    }

    /**
     * @param req    datos
     * @param motivo CREACION o MIGRACION
     * @return producto creado
     */
    @Transactional
    public ProductoResponse crear(ProductoRequest req, HistorialPrecio.Motivo motivo) {
        String codigo = req.codigoBarras().trim();
        if (productoRepository.existsByCodigoBarras(codigo)) {
            throw new ConflictoException("producto.codigo.duplicado", codigo);
        }
        PrecioService.validar(req.precioCompra(), req.porcentajeIncremento());
        Producto p = new Producto();
        aplicar(p, req);
        p.setPrecioCompra(req.precioCompra());
        p.setPorcentajeIncremento(req.porcentajeIncremento());
        p.setPrecioVenta(PrecioService.calcularPrecioVenta(req.precioCompra(), req.porcentajeIncremento()));
        productoRepository.saveAndFlush(p);
        precioService.aplicar(p, req.precioCompra(), req.porcentajeIncremento(), motivo, null);
        inventarioService.inicializarSaldo(p.getId());
        return respuesta(p, null);
    }

    /**
     * @param id  id
     * @param req datos
     * @return producto actualizado
     */
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = entidad(id);
        String codigo = req.codigoBarras().trim();
        if (productoRepository.existsByCodigoBarrasAndIdNot(codigo, id)) {
            throw new ConflictoException("producto.codigo.duplicado", codigo);
        }
        if (!p.getUnidadBase().getId().equals(req.unidadBaseId()) && tieneExistencias(id)) {
            throw new ReglaNegocioException("producto.unidad.base.con.stock");
        }
        aplicar(p, req);
        precioService.aplicar(p, req.precioCompra(), req.porcentajeIncremento(),
                HistorialPrecio.Motivo.ACTUALIZACION_MANUAL, null);
        return respuesta(p, null);
    }

    /**
     * @param id            id
     * @param precioCompra  costo
     * @param porcentaje    porcentaje
     * @return producto con precio recalculado
     */
    @Transactional
    public ProductoResponse actualizarPrecio(Long id, BigDecimal precioCompra, BigDecimal porcentaje) {
        Producto p = entidad(id);
        precioService.aplicar(p, precioCompra, porcentaje, HistorialPrecio.Motivo.ACTUALIZACION_MANUAL, null);
        return respuesta(p, null);
    }

    /**
     * @param id     id
     * @param estado nuevo estado de catálogo
     * @return producto
     */
    @Transactional
    public ProductoResponse cambiarEstado(Long id, EstadoProducto estado) {
        Producto p = entidad(id);
        p.setEstado(estado);
        return respuesta(p, null);
    }

    /**
     * Eliminación lógica (estado INACTIVO): conserva la trazabilidad histórica.
     *
     * @param id id
     */
    @Transactional
    public void eliminar(Long id) {
        entidad(id).setEstado(EstadoProducto.INACTIVO);
    }

    /**
     * @param id id
     * @return producto
     */
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return respuesta(entidad(id), null);
    }

    /**
     * @param codigo código de barras
     * @return producto (CA-13.1)
     */
    @Transactional(readOnly = true)
    public ProductoResponse porCodigoBarras(String codigo) {
        Producto p = productoRepository.findByCodigoBarras(codigo.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.producto", codigo));
        return respuesta(p, null);
    }

    /**
     * @param nombre      texto en nombre o código
     * @param categoriaId categoría
     * @param marcaId     marca
     * @param estado      estado
     * @param pagina      página
     * @param tamano      tamaño
     * @param orden       campo de orden
     * @return página de productos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listar(String nombre, Long categoriaId, Long marcaId, EstadoProducto estado,
                                                   int pagina, int tamano, String orden) {
        Specification<Producto> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                String like = "%" + nombre.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.<String>get("nombre")), like),
                        cb.like(cb.lower(root.<String>get("codigoBarras")), like)));
            }
            if (categoriaId != null) {
                p.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (marcaId != null) {
                p.add(cb.equal(root.get("marca").get("id"), marcaId));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        String campo = List.of("nombre", "codigoBarras", "precioVenta", "id", "actualizadoEn").contains(orden)
                ? orden : "nombre";
        Page<Producto> page = productoRepository.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 500),
                Sort.by(campo)));
        Map<Long, InventarioSaldo> saldos = saldoRepository.findByProductoIdIn(
                        page.getContent().stream().map(Producto::getId).toList()).stream()
                .collect(Collectors.toMap(InventarioSaldo::getProductoId, Function.identity()));
        return PaginaResponse.de(page, p -> respuesta(p, saldos.get(p.getId())));
    }

    /**
     * @param id producto
     * @return historial de costos y precios
     */
    @Transactional(readOnly = true)
    public List<HistorialPrecioResponse> historial(Long id) {
        entidad(id);
        return historialRepository.findByProductoIdOrderByFechaDescIdDesc(id).stream()
                .map(HistorialPrecioResponse::de).toList();
    }

    /**
     * @param id id
     * @return entidad con relaciones
     */
    @Transactional(readOnly = true)
    public Producto entidad(Long id) {
        return productoRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.producto", id));
    }

    /**
     * Convierte la entidad al contrato REST.
     *
     * @param p     producto
     * @param saldo saldo precargado (null = consultar)
     * @return DTO
     */
    public ProductoResponse respuesta(Producto p, InventarioSaldo saldo) {
        InventarioSaldo s = saldo != null ? saldo : saldoRepository.findById(p.getId()).orElse(null);
        Existencias ex = s == null
                ? new Existencias(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                : new Existencias(s.getDisponible(), s.getReservada(), s.getDanada(), s.total());
        LocalDate vence = p.isTieneVencimiento() ? loteRepository.proximoVencimiento(p.getId()).orElse(null) : null;
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria().getNombre(), p.getDescripcion(),
                p.getEstado().name(), p.getCodigoBarras(), p.getMarca().getNombre(), p.getPresentacion().getNombre(),
                p.isPermiteFraccion(), p.getPrecioCompra(), p.getPorcentajeIncremento(), p.getPrecioVenta(),
                p.isTieneGarantia(), p.getMesesGarantia(), p.isTieneVencimiento(), vence,
                p.getCategoria().getId(), p.getMarca().getId(), p.getPresentacion().getId(), p.getUnidadBase().getId(),
                p.getUnidadBase().getCodigo(), p.getPorcentajeIva(), p.getDiasAlertaVencimiento(), p.getStockMinimo(),
                ex, p.getActualizadoEn());
    }

    private boolean tieneExistencias(Long id) {
        return saldoRepository.findById(id).map(s -> s.total().signum() > 0).orElse(false);
    }

    private void aplicar(Producto p, ProductoRequest req) {
        if (req.tieneGarantia() && req.mesesGarantia() == null) {
            throw new ReglaNegocioException("producto.garantia.meses");
        }
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setCodigoBarras(req.codigoBarras().trim());
        p.setCategoria(categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new ReglaNegocioException("producto.categoria.invalida")));
        p.setMarca(marcaRepository.findById(req.marcaId())
                .orElseThrow(() -> new ReglaNegocioException("producto.marca.invalida")));
        p.setPresentacion(presentacionRepository.findById(req.presentacionId())
                .orElseThrow(() -> new ReglaNegocioException("producto.presentacion.invalida")));
        p.setUnidadBase(unidadRepository.findById(req.unidadBaseId())
                .orElseThrow(() -> new ReglaNegocioException("producto.unidad.invalida")));
        p.setPermiteFraccion(req.permiteFraccion());
        p.setPorcentajeIva(req.porcentajeIva() == null ? BigDecimal.ZERO : req.porcentajeIva());
        p.setTieneGarantia(req.tieneGarantia());
        p.setMesesGarantia(req.tieneGarantia() ? req.mesesGarantia() : null);
        p.setTieneVencimiento(req.tieneVencimiento());
        p.setDiasAlertaVencimiento(req.diasAlertaVencimiento() == null ? 30 : req.diasAlertaVencimiento());
        p.setStockMinimo(req.stockMinimo() == null ? BigDecimal.ZERO : req.stockMinimo());
    }
}
