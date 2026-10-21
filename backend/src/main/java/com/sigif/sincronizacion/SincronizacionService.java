/*
 * Nombre: SincronizacionService.java
 * Descripción: Orquesta la sincronización de ventas offline y expone el catálogo para operar sin internet.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.sincronizacion;

import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.SigifException;
import com.sigif.common.util.Mensajes;
import com.sigif.common.util.UsuarioActual;
import com.sigif.inventario.InventarioRepositories.InventarioSaldoRepository;
import com.sigif.inventario.InventarioSaldo;
import com.sigif.producto.ConversionUnidad;
import com.sigif.producto.EstadoProducto;
import com.sigif.producto.Producto;
import com.sigif.producto.ProductoRepositories.ConversionUnidadRepository;
import com.sigif.producto.ProductoRepositories.ProductoRepository;
import com.sigif.sincronizacion.SincronizacionDtos.CatalogoOffline;
import com.sigif.sincronizacion.SincronizacionDtos.ClienteOffline;
import com.sigif.sincronizacion.SincronizacionDtos.ConversionOffline;
import com.sigif.sincronizacion.SincronizacionDtos.OperacionResponse;
import com.sigif.sincronizacion.SincronizacionDtos.ProductoOffline;
import com.sigif.sincronizacion.SincronizacionDtos.ResultadoSincronizacion;
import com.sigif.sincronizacion.SincronizacionDtos.SincronizacionRequest;
import com.sigif.sincronizacion.SincronizacionDtos.VentaOffline;
import com.sigif.tercero.TerceroRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Política de conflictos (pregunta 11.4 #14): el servidor es la fuente de verdad. Las ventas se
 * aplican en orden de fecha local; la primera que encuentre stock lo consume y las siguientes
 * quedan en CONFLICTO para revisión de un supervisor (reintentar o descartar).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionService {

    private final SincronizacionProcesador procesador;
    private final SincronizacionRepository repository;
    private final ProductoRepository productoRepository;
    private final ConversionUnidadRepository conversionRepository;
    private final InventarioSaldoRepository saldoRepository;
    private final TerceroRepository terceroRepository;
    private final Mensajes mensajes;

    /**
     * @param req lote de ventas offline
     * @return resultado por venta
     */
    public List<ResultadoSincronizacion> sincronizar(SincronizacionRequest req) {
        List<ResultadoSincronizacion> resultados = new ArrayList<>();
        List<VentaOffline> ordenadas = req.ventas().stream()
                .sorted(Comparator.comparing(VentaOffline::fechaLocal)).toList();
        for (VentaOffline v : ordenadas) {
            Optional<ResultadoSincronizacion> previo = procesador.previo(v.idLocal());
            if (previo.isPresent()) {
                resultados.add(previo.get());
                continue;
            }
            try {
                resultados.add(procesador.aplicar(v));
            } catch (SigifException ex) {
                resultados.add(procesador.conflicto(v, mensajes.obtener(ex.getClave(), ex.getArgumentos())));
            } catch (RuntimeException ex) {
                log.warn("Venta offline {} rechazada: {}", v.idLocal(), ex.getMessage());
                resultados.add(procesador.conflicto(v, mensajes.obtener("sincronizacion.error.aplicar")));
            }
        }
        return resultados;
    }

    /**
     * @return catálogo de productos activos y clientes para la caché local
     */
    @Transactional(readOnly = true)
    public CatalogoOffline catalogo() {
        List<Producto> productos = productoRepository.findByEstado(EstadoProducto.ACTIVO);
        List<Long> ids = productos.stream().map(Producto::getId).toList();
        Map<Long, InventarioSaldo> saldos = saldoRepository.findByProductoIdIn(ids).stream()
                .collect(Collectors.toMap(InventarioSaldo::getProductoId, Function.identity()));
        Map<Long, List<ConversionUnidad>> conversiones = conversionRepository.findByProductoIdIn(ids).stream()
                .filter(ConversionUnidad::isUsoVenta)
                .collect(Collectors.groupingBy(c -> c.getProducto().getId()));
        List<ProductoOffline> items = productos.stream().map(p -> new ProductoOffline(p.getId(), p.getCodigoBarras(),
                p.getNombre(), p.getMarca().getNombre(), p.getUnidadBase().getId(), p.getUnidadBase().getCodigo(),
                p.isPermiteFraccion(), p.getPrecioVenta(), p.getPorcentajeIva(),
                saldos.containsKey(p.getId()) ? saldos.get(p.getId()).getDisponible() : BigDecimal.ZERO,
                p.isTieneGarantia(), p.getMesesGarantia(),
                conversiones.getOrDefault(p.getId(), List.of()).stream()
                        .map(c -> new ConversionOffline(c.getUnidad().getId(), c.getUnidad().getCodigo(), c.getFactor()))
                        .toList())).toList();
        List<ClienteOffline> clientes = terceroRepository.findAll(
                        (root, q, cb) -> cb.and(cb.isTrue(root.<Boolean>get("esCliente")), cb.isTrue(root.<Boolean>get("activo"))),
                        PageRequest.of(0, 2000, Sort.by("id"))).stream()
                .map(t -> new ClienteOffline(t.getId(), t.getNumeroDocumento(), t.nombreMostrar())).toList();
        return new CatalogoOffline(OffsetDateTime.now(), items, clientes);
    }

    /**
     * @param estado filtro (null = todas)
     * @return operaciones recientes
     */
    @Transactional(readOnly = true)
    public List<OperacionResponse> operaciones(SincronizacionOperacion.Estado estado) {
        List<SincronizacionOperacion> lista = estado == null ? repository.findTop200ByOrderByIdDesc()
                : repository.findTop200ByEstadoOrderByIdDesc(estado);
        return lista.stream().map(OperacionResponse::de).toList();
    }

    /**
     * @param id operación en conflicto
     * @return operación resuelta
     */
    public OperacionResponse reintentar(Long id) {
        validarConflicto(id);
        return OperacionResponse.de(procesador.reintentar(id));
    }

    /**
     * @param id operación en conflicto
     * @return operación descartada
     */
    @Transactional
    public OperacionResponse descartar(Long id) {
        SincronizacionOperacion o = validarConflicto(id);
        o.setEstado(SincronizacionOperacion.Estado.DESCARTADA);
        o.setResueltoPor(UsuarioActual.nombre());
        o.setFechaResolucion(OffsetDateTime.now());
        return OperacionResponse.de(o);
    }

    private SincronizacionOperacion validarConflicto(Long id) {
        SincronizacionOperacion o = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.operacion", id));
        if (o.getEstado() != SincronizacionOperacion.Estado.CONFLICTO) {
            throw new ConflictoException("sincronizacion.no.conflicto", id);
        }
        return o;
    }
}
