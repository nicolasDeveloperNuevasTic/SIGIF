/*
 * Nombre: InventarioConsultaService.java
 * Descripción: Consultas de existencias, kardex, lotes y alertas; transiciones manuales de estado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.cierre.PeriodoService;
import com.sigif.common.dto.PaginaResponse;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.inventario.InventarioDtos.LoteResponse;
import com.sigif.inventario.InventarioDtos.MovimientoResponse;
import com.sigif.inventario.InventarioDtos.SaldoResponse;
import com.sigif.inventario.InventarioDtos.TransicionRequest;
import com.sigif.inventario.InventarioRepositories.InventarioSaldoRepository;
import com.sigif.inventario.InventarioRepositories.LoteRepository;
import com.sigif.inventario.InventarioRepositories.MovimientoInventarioRepository;
import com.sigif.producto.EstadoProducto;
import com.sigif.producto.Producto;
import com.sigif.producto.ProductoService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecturas del inventario y transiciones DISPONIBLE ↔ DANADA solicitadas por el Jefe de Bodega.
 * Las transiciones hacia/desde RESERVADA se gestionan con {@link ReservaService}.
 */
@Service
@RequiredArgsConstructor
public class InventarioConsultaService {

    private final InventarioSaldoRepository saldoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final LoteRepository loteRepository;
    private final InventarioService inventarioService;
    private final ProductoService productoService;
    private final PeriodoService periodoService;

    /**
     * @param texto        filtro por nombre o código
     * @param soloConStock solo productos con existencias
     * @param pagina       página
     * @param tamano       tamaño
     * @return página de saldos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<SaldoResponse> saldos(String texto, boolean soloConStock, int pagina, int tamano) {
        Specification<InventarioSaldo> spec = (root, q, cb) -> {
            var producto = root.join("producto", JoinType.INNER);
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(producto.get("estado"), EstadoProducto.ACTIVO));
            if (texto != null && !texto.isBlank()) {
                String like = "%" + texto.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(producto.<String>get("nombre")), like),
                        cb.like(cb.lower(producto.<String>get("codigoBarras")), like)));
            }
            if (soloConStock) {
                p.add(cb.gt(cb.sum(cb.sum(root.<java.math.BigDecimal>get("disponible"), root.<java.math.BigDecimal>get("reservada")), root.<java.math.BigDecimal>get("danada")), 0));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(saldoRepository.findAll(spec,
                PageRequest.of(pagina, Math.min(tamano, 500), Sort.by("producto.nombre"))), SaldoResponse::de);
    }

    /**
     * @param productoId producto
     * @param tipo       tipo de movimiento
     * @param desde      desde
     * @param hasta      hasta
     * @param pagina     página
     * @param tamano     tamaño
     * @return kardex paginado
     */
    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> movimientos(Long productoId, MovimientoInventario.Tipo tipo,
                                                          OffsetDateTime desde, OffsetDateTime hasta,
                                                          int pagina, int tamano) {
        Specification<MovimientoInventario> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (productoId != null) {
                p.add(cb.equal(root.get("producto").get("id"), productoId));
            }
            if (tipo != null) {
                p.add(cb.equal(root.get("tipo"), tipo));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("fecha"), desde));
            }
            if (hasta != null) {
                p.add(cb.lessThanOrEqualTo(root.<OffsetDateTime>get("fecha"), hasta));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(movimientoRepository.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 500),
                Sort.by(Sort.Direction.DESC, "fecha", "id"))), MovimientoResponse::de);
    }

    /**
     * @param productoId producto
     * @return lotes del producto
     */
    @Transactional(readOnly = true)
    public List<LoteResponse> lotes(Long productoId) {
        LocalDate hoy = hoy();
        return loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId).stream()
                .map(l -> LoteResponse.de(l, hoy)).toList();
    }

    /**
     * @param dias horizonte en días
     * @return lotes vencidos o por vencer con existencias
     */
    @Transactional(readOnly = true)
    public List<LoteResponse> alertasVencimiento(int dias) {
        LocalDate hoy = hoy();
        return loteRepository.porVencer(hoy.plusDays(dias)).stream().map(l -> LoteResponse.de(l, hoy)).toList();
    }

    /**
     * @return productos en o bajo el stock mínimo
     */
    @Transactional(readOnly = true)
    public List<SaldoResponse> alertasStockMinimo() {
        return saldoRepository.bajoStockMinimo().stream().map(SaldoResponse::de).toList();
    }

    /**
     * Transición manual auditada (REQ-F-014).
     *
     * @param req transición
     * @return saldo resultante
     */
    @Transactional
    public SaldoResponse transicion(TransicionRequest req) {
        if (req.estadoOrigen() == EstadoInventario.RESERVADA || req.estadoDestino() == EstadoInventario.RESERVADA) {
            throw new ReglaNegocioException("inventario.transicion.reserva");
        }
        Producto p = productoService.entidad(req.productoId());
        if (!p.isPermiteFraccion() && req.cantidad().stripTrailingZeros().scale() > 0) {
            throw new ReglaNegocioException("producto.no.fraccionable", p.getNombre());
        }
        InventarioSaldo s = inventarioService.cambiarEstado(p, req.cantidad(), req.estadoOrigen(),
                req.estadoDestino(), req.motivo(), null);
        s.setProducto(p);
        return SaldoResponse.de(s);
    }

    private LocalDate hoy() {
        return LocalDate.now(periodoService.zona());
    }
}
