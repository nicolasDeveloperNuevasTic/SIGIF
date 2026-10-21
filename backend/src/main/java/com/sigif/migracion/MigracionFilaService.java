/*
 * Nombre: MigracionFilaService.java
 * Descripción: Importa una fila de Excel en su propia transacción (fallo aislado por fila).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.migracion;

import com.sigif.catalogo.CatalogoService;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.compra.CompraDtos.CompraRequest;
import com.sigif.compra.CompraDtos.LineaCompra;
import com.sigif.compra.CompraService;
import com.sigif.producto.HistorialPrecio;
import com.sigif.producto.ProductoDtos.ProductoRequest;
import com.sigif.producto.ProductoRepositories.ProductoRepository;
import com.sigif.producto.ProductoService;
import com.sigif.tercero.Tercero;
import com.sigif.tercero.TerceroDtos.TerceroRequest;
import com.sigif.tercero.TerceroRepository;
import com.sigif.tercero.TerceroService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cada método se ejecuta con REQUIRES_NEW: una fila inválida se revierte sin afectar las demás
 * ni corromper la base de datos (REQ-F-037). Reutiliza los servicios de dominio, por lo que se
 * aplican las mismas reglas que en la operación normal (código de barras único, stock &gt;= 0).
 */
@Service
@RequiredArgsConstructor
public class MigracionFilaService {

    private final CatalogoService catalogoService;
    private final ProductoService productoService;
    private final ProductoRepository productoRepository;
    private final TerceroService terceroService;
    private final TerceroRepository terceroRepository;
    private final CompraService compraService;
    private final Validator validator;

    /**
     * Columnas: codigoBarras, nombre, categoria, marca, presentacion, unidadBase, descripcion,
     * permiteFraccion, precioCompra, porcentajeIncremento, porcentajeIva, tieneGarantia,
     * mesesGarantia, tieneVencimiento, stockMinimo.
     *
     * @param f fila
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void producto(FilaExcel f) {
        ProductoRequest req = new ProductoRequest(
                f.texto("nombre", true),
                catalogoService.categoria(f.texto("categoria", true)).getId(),
                f.texto("descripcion", false),
                f.texto("codigoBarras", true),
                catalogoService.marca(f.texto("marca", true)).getId(),
                catalogoService.presentacion(f.texto("presentacion", true)).getId(),
                catalogoService.unidadPorCodigo(f.texto("unidadBase", true)).getId(),
                f.booleano("permiteFraccion"),
                f.decimal("precioCompra", true),
                f.decimal("porcentajeIncremento", true),
                f.decimal("porcentajeIva", false),
                f.booleano("tieneGarantia"),
                f.entero("mesesGarantia"),
                f.booleano("tieneVencimiento"),
                null,
                f.decimal("stockMinimo", false));
        validar(req);
        productoService.crear(req, HistorialPrecio.Motivo.MIGRACION);
    }

    /**
     * Columnas: tipoPersona, tipoDocumento, numeroDocumento, digitoVerificacion, nombre, apellido,
     * razonSocial, direccion, telefono, correo, esCliente, esProveedor.
     *
     * @param f fila
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tercero(FilaExcel f) {
        TerceroRequest req = new TerceroRequest(
                Tercero.TipoPersona.valueOf(f.texto("tipoPersona", true).toUpperCase()),
                Tercero.TipoDocumento.valueOf(f.texto("tipoDocumento", true).toUpperCase()),
                f.texto("numeroDocumento", true),
                f.texto("digitoVerificacion", false),
                f.texto("nombre", false),
                f.texto("apellido", false),
                f.texto("razonSocial", false),
                f.texto("direccion", true),
                f.texto("telefono", true),
                f.texto("correo", true),
                f.booleano("esCliente"),
                f.booleano("esProveedor"));
        validar(req);
        terceroService.crear(req);
    }

    /**
     * Inventario inicial como entrada por compra al proveedor genérico SALDO INICIAL (RN-001).
     * Columnas: codigoBarras, cantidad, costoUnitario, codigoLote, fechaVencimiento.
     *
     * @param f fila
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inventario(FilaExcel f) {
        String codigo = f.texto("codigoBarras", true);
        Long productoId = productoRepository.findByCodigoBarras(codigo)
                .orElseThrow(() -> new ReglaNegocioException("migracion.producto.no.existe", codigo)).getId();
        BigDecimal cantidad = f.decimal("cantidad", true);
        if (cantidad.signum() < 0) {
            throw new ReglaNegocioException("migracion.existencia.negativa");
        }
        if (cantidad.signum() == 0) {
            return;
        }
        Tercero saldoInicial = terceroRepository.findByNumeroDocumento(TerceroService.SALDO_INICIAL).orElseThrow();
        LocalDate vence = f.fecha("fechaVencimiento");
        LineaCompra linea = new LineaCompra(productoId, null, cantidad, f.decimal("costoUnitario", true),
                BigDecimal.ZERO, f.texto("codigoLote", false), vence);
        compraService.registrar(new CompraRequest(saldoInicial.getId(), null, "Migración Excel - inventario inicial",
                List.of(linea)));
    }

    private <T> void validar(T req) {
        Set<ConstraintViolation<T>> errores = validator.validate(req);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(errores.stream()
                    .map(e -> e.getPropertyPath() + ": " + e.getMessage()).sorted().collect(Collectors.joining("; ")));
        }
    }
}
