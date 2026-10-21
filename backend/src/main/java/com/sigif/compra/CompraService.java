/*
 * Nombre: CompraService.java
 * Descripción: Registro de entradas por compra, actualización de costo promedio y generación
 *              automática del Documento de Ingreso.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.compra;

import com.sigif.cierre.PeriodoService;
import com.sigif.common.dto.PaginaResponse;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.numeracion.ConsecutivoService;
import com.sigif.common.util.Numeros;
import com.sigif.common.util.UsuarioActual;
import com.sigif.compra.CompraDtos.CompraRequest;
import com.sigif.compra.CompraDtos.DocumentoIngresoResponse;
import com.sigif.compra.CompraDtos.LineaCompra;
import com.sigif.inventario.InventarioRepositories.LoteRepository;
import com.sigif.inventario.InventarioSaldo;
import com.sigif.inventario.InventarioService;
import com.sigif.inventario.Lote;
import com.sigif.producto.ConversionService;
import com.sigif.producto.ConversionService.Conversion;
import com.sigif.producto.HistorialPrecio;
import com.sigif.producto.PrecioService;
import com.sigif.producto.Producto;
import com.sigif.producto.ProductoService;
import com.sigif.tercero.Tercero;
import com.sigif.tercero.TerceroService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo REQ-F-018 a REQ-F-020: valida proveedor y cantidades, incrementa DISPONIBLE, recalcula
 * costo promedio ponderado y precio de venta (con historial) y emite el Documento de Ingreso.
 */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final DocumentoIngresoRepository documentoRepository;
    private final LoteRepository loteRepository;
    private final TerceroService terceroService;
    private final ProductoService productoService;
    private final ConversionService conversionService;
    private final PrecioService precioService;
    private final InventarioService inventarioService;
    private final ConsecutivoService consecutivoService;
    private final PeriodoService periodoService;

    /**
     * @param req compra
     * @return Documento de Ingreso generado
     */
    @Transactional
    public DocumentoIngresoResponse registrar(CompraRequest req) {
        Tercero proveedor = terceroService.proveedorValido(req.proveedorId());
        OffsetDateTime fecha = OffsetDateTime.now();
        int periodo = periodoService.periodo(fecha);
        periodoService.validarAbierto(periodo);

        DocumentoIngreso doc = new DocumentoIngreso();
        doc.setNumero(consecutivoService.siguiente(ConsecutivoService.DOC_INGRESO));
        doc.setProveedor(proveedor);
        doc.setFecha(fecha);
        doc.setPeriodo(periodo);
        doc.setNumeroFacturaProveedor(req.numeroFacturaProveedor());
        doc.setObservaciones(req.observaciones());
        doc.setUsuario(UsuarioActual.nombre());
        documentoRepository.save(doc);

        // Orden por producto para adquirir bloqueos siempre en el mismo orden (evita interbloqueos).
        List<LineaCompra> lineas = req.lineas().stream()
                .sorted(Comparator.comparing(LineaCompra::productoId)).toList();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        for (LineaCompra linea : lineas) {
            DetalleIngreso d = procesarLinea(doc, linea, fecha);
            subtotal = subtotal.add(d.getSubtotal());
            iva = iva.add(d.getValorIva());
        }
        doc.setSubtotal(Numeros.moneda(subtotal));
        doc.setTotalIva(Numeros.moneda(iva));
        doc.setTotal(Numeros.moneda(subtotal.add(iva)));
        return DocumentoIngresoResponse.de(doc);
    }

    private DetalleIngreso procesarLinea(DocumentoIngreso doc, LineaCompra linea, OffsetDateTime fecha) {
        Producto p = productoService.entidad(linea.productoId());
        if (!p.esActivo()) {
            throw new ReglaNegocioException("producto.inactivo", p.getNombre());
        }
        if (linea.costoUnitario() == null || linea.costoUnitario().signum() <= 0) {
            throw new ReglaNegocioException("precio.compra.invalido");
        }
        Conversion conv = conversionService.convertir(p, linea.unidadId(), linea.cantidad(), ConversionService.Uso.COMPRA);
        BigDecimal costoBase = linea.costoUnitario().divide(conv.factor(), Numeros.ESCALA_PRECIO, RoundingMode.HALF_UP);

        InventarioSaldo saldo = inventarioService.bloquear(p.getId());
        BigDecimal nuevoCosto = PrecioService.costoPromedio(saldo.total(), p.getPrecioCompra(), conv.cantidadBase(),
                costoBase);
        precioService.aplicar(p, nuevoCosto, p.getPorcentajeIncremento(), HistorialPrecio.Motivo.COMPRA, doc.getNumero());

        Lote lote = p.isTieneVencimiento() ? registrarLote(p, linea, conv.cantidadBase(), doc, fecha) : null;
        inventarioService.registrarEntrada(saldo, p, conv.cantidadBase(), costoBase, doc, lote, fecha);

        BigDecimal pctIva = linea.porcentajeIva() == null ? BigDecimal.ZERO : linea.porcentajeIva();
        DetalleIngreso d = new DetalleIngreso();
        d.setDocumento(doc);
        d.setProducto(p);
        d.setUnidad(conv.unidad());
        d.setCantidad(linea.cantidad());
        d.setFactorConversion(conv.factor());
        d.setCantidadBase(conv.cantidadBase());
        d.setCostoUnitario(Numeros.precio(linea.costoUnitario()));
        d.setPorcentajeIva(pctIva);
        d.setSubtotal(Numeros.moneda(linea.cantidad().multiply(linea.costoUnitario())));
        d.setValorIva(Numeros.porcentaje(d.getSubtotal(), pctIva));
        d.setTotal(d.getSubtotal().add(d.getValorIva()));
        d.setLote(lote);
        doc.getDetalles().add(d);
        return d;
    }

    private Lote registrarLote(Producto p, LineaCompra linea, BigDecimal cantidadBase, DocumentoIngreso doc,
                               OffsetDateTime fecha) {
        if (linea.codigoLote() == null || linea.codigoLote().isBlank() || linea.fechaVencimiento() == null) {
            throw new ReglaNegocioException("compra.lote.obligatorio", p.getNombre());
        }
        LocalDate hoy = fecha.atZoneSameInstant(periodoService.zona()).toLocalDate();
        if (linea.fechaVencimiento().isBefore(hoy)) {
            throw new ReglaNegocioException("compra.lote.vencido", p.getNombre());
        }
        Lote lote = loteRepository.findByProductoIdAndCodigoLote(p.getId(), linea.codigoLote().trim()).orElse(null);
        if (lote == null) {
            lote = new Lote();
            lote.setProducto(p);
            lote.setCodigoLote(linea.codigoLote().trim());
            lote.setFechaVencimiento(linea.fechaVencimiento());
            lote.setCantidadInicial(cantidadBase);
            lote.setCantidadActual(cantidadBase);
            lote.setDocumentoIngreso(doc);
            return loteRepository.save(lote);
        }
        if (!lote.getFechaVencimiento().equals(linea.fechaVencimiento())) {
            throw new ReglaNegocioException("compra.lote.fecha.distinta", lote.getCodigoLote());
        }
        lote.setCantidadInicial(lote.getCantidadInicial().add(cantidadBase));
        lote.setCantidadActual(lote.getCantidadActual().add(cantidadBase));
        return lote;
    }

    /**
     * @param id id
     * @return documento con detalle
     */
    @Transactional(readOnly = true)
    public DocumentoIngresoResponse obtener(Long id) {
        return documentoRepository.findConDetalleById(id).map(DocumentoIngresoResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.documento.ingreso", id));
    }

    /**
     * @param proveedorId proveedor
     * @param numero      número
     * @param desde       desde
     * @param hasta       hasta
     * @param pagina      página
     * @param tamano      tamaño
     * @return documentos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<DocumentoIngresoResponse> buscar(Long proveedorId, String numero, OffsetDateTime desde,
                                                           OffsetDateTime hasta, int pagina, int tamano) {
        Specification<DocumentoIngreso> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (proveedorId != null) {
                p.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
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
        return PaginaResponse.de(documentoRepository.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 200),
                Sort.by(Sort.Direction.DESC, "id"))), DocumentoIngresoResponse::resumen);
    }
}
