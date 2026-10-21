/*
 * Nombre: VentaService.java
 * Descripción: Registro de salidas por venta (unidades o fracciones), validación de stock,
 *              garantías y generación automática de la Factura.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.venta;

import com.sigif.cierre.PeriodoService;
import com.sigif.common.dto.PaginaResponse;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.numeracion.ConsecutivoService;
import com.sigif.common.util.Numeros;
import com.sigif.common.util.UsuarioActual;
import com.sigif.inventario.EstadoInventario;
import com.sigif.inventario.InventarioService;
import com.sigif.inventario.Reserva;
import com.sigif.inventario.ReservaService;
import com.sigif.producto.ConversionService;
import com.sigif.producto.ConversionService.Conversion;
import com.sigif.producto.Producto;
import com.sigif.producto.ProductoService;
import com.sigif.tercero.Tercero;
import com.sigif.tercero.TerceroService;
import com.sigif.venta.VentaDtos.FacturaResponse;
import com.sigif.venta.VentaDtos.LineaVenta;
import com.sigif.venta.VentaDtos.VentaRequest;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo REQ-F-021 a REQ-F-024. Toda la venta es atómica: si una línea falla (stock insuficiente,
 * producto vencido, unidades dañadas) se revierte la factura completa (rollback).
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final FacturaRepository facturaRepository;
    private final TerceroService terceroService;
    private final ProductoService productoService;
    private final ConversionService conversionService;
    private final InventarioService inventarioService;
    private final ReservaService reservaService;
    private final ConsecutivoService consecutivoService;
    private final PeriodoService periodoService;

    /**
     * @param req venta
     * @return factura generada
     */
    @Transactional
    public FacturaResponse registrar(VentaRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean integracion = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INTEGRATION".equals(a.getAuthority()));
        return FacturaResponse.de(registrar(req, integracion ? Factura.Origen.API : Factura.Origen.EN_LINEA, null));
    }

    /**
     * @param req     venta
     * @param origen  canal de origen
     * @param idLocal UUID local (ventas offline) o null
     * @return factura persistida
     */
    @Transactional
    public Factura registrar(VentaRequest req, Factura.Origen origen, UUID idLocal) {
        Tercero cliente = terceroService.clienteValido(req.clienteId());
        OffsetDateTime fecha = OffsetDateTime.now();
        int periodo = periodoService.periodo(fecha);
        periodoService.validarAbierto(periodo);

        Factura f = new Factura();
        f.setNumero(consecutivoService.siguiente(ConsecutivoService.FACTURA));
        f.setCliente(cliente);
        f.setFecha(fecha);
        f.setPeriodo(periodo);
        f.setOrigen(origen);
        f.setIdLocalOffline(idLocal);
        f.setObservaciones(req.observaciones());
        f.setUsuario(UsuarioActual.nombre());
        facturaRepository.save(f);

        List<LineaVenta> lineas = req.lineas().stream()
                .sorted(Comparator.comparing(LineaVenta::productoId)).toList();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        for (LineaVenta linea : lineas) {
            DetalleFactura d = procesarLinea(f, cliente, linea, fecha);
            subtotal = subtotal.add(d.getSubtotal());
            descuento = descuento.add(d.getValorDescuento());
            iva = iva.add(d.getValorIva());
        }
        f.setSubtotal(Numeros.moneda(subtotal));
        f.setTotalDescuento(Numeros.moneda(descuento));
        f.setTotalIva(Numeros.moneda(iva));
        f.setTotal(Numeros.moneda(subtotal.subtract(descuento).add(iva)));
        return f;
    }

    private DetalleFactura procesarLinea(Factura f, Tercero cliente, LineaVenta linea, OffsetDateTime fecha) {
        Producto p = productoService.entidad(linea.productoId());
        if (!p.esActivo()) {
            throw new ReglaNegocioException("producto.inactivo", p.getNombre());
        }
        Conversion conv = conversionService.convertir(p, linea.unidadId(), linea.cantidad(), ConversionService.Uso.VENTA);

        Reserva reserva = null;
        EstadoInventario origen = EstadoInventario.DISPONIBLE;
        if (linea.reservaId() != null) {
            reserva = reservaService.consumir(linea.reservaId(), cliente.getId(), p.getId(), conv.cantidadBase());
            origen = EstadoInventario.RESERVADA;
        }
        inventarioService.registrarSalida(p, conv.cantidadBase(), origen, f, reserva, fecha);

        BigDecimal pctDesc = linea.porcentajeDescuento() == null ? BigDecimal.ZERO : linea.porcentajeDescuento();
        DetalleFactura d = new DetalleFactura();
        d.setFactura(f);
        d.setProducto(p);
        d.setUnidad(conv.unidad());
        d.setCantidad(linea.cantidad());
        d.setFactorConversion(conv.factor());
        d.setCantidadBase(conv.cantidadBase());
        d.setPrecioUnitario(Numeros.precio(p.getPrecioVenta().multiply(conv.factor())));
        d.setPorcentajeDescuento(pctDesc);
        d.setPorcentajeIva(p.getPorcentajeIva());
        d.setSubtotal(Numeros.moneda(p.getPrecioVenta().multiply(conv.cantidadBase())));
        d.setValorDescuento(Numeros.porcentaje(d.getSubtotal(), pctDesc));
        d.setValorIva(Numeros.porcentaje(d.getSubtotal().subtract(d.getValorDescuento()), p.getPorcentajeIva()));
        d.setTotal(d.getSubtotal().subtract(d.getValorDescuento()).add(d.getValorIva()));
        d.setReserva(reserva);
        d.setNumeroSerie(linea.numeroSerie());
        if (p.isTieneGarantia()) {
            d.setMesesGarantia(p.getMesesGarantia());
            d.setFechaFinGarantia(fecha.atZoneSameInstant(periodoService.zona()).toLocalDate()
                    .plusMonths(p.getMesesGarantia()));
        }
        f.getDetalles().add(d);
        return d;
    }

    /**
     * @param id id
     * @return factura con detalle
     */
    @Transactional(readOnly = true)
    public FacturaResponse obtener(Long id) {
        return facturaRepository.findConDetalleById(id).map(FacturaResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.factura", id));
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
    @Transactional(readOnly = true)
    public PaginaResponse<FacturaResponse> buscar(Long clienteId, String numero, OffsetDateTime desde,
                                                  OffsetDateTime hasta, int pagina, int tamano) {
        Specification<Factura> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (numero != null && !numero.isBlank()) {
                p.add(cb.like(cb.upper(root.<String>get("numero")), "%" + numero.trim().toUpperCase() + "%"));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("fecha"), desde));
            }
            if (hasta != null) {
                p.add(cb.lessThanOrEqualTo(root.<OffsetDateTime>get("fecha"), hasta));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(facturaRepository.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 200),
                Sort.by(Sort.Direction.DESC, "id"))), FacturaResponse::resumen);
    }
}
