/*
 * Nombre: ConversionService.java
 * Descripción: Conversión de cantidades a unidad base y gestión de factores de conversión.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import com.sigif.catalogo.CatalogoRepositories.UnidadMedidaRepository;
import com.sigif.catalogo.UnidadMedida;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.util.Numeros;
import com.sigif.producto.ProductoRepositories.ConversionUnidadRepository;
import com.sigif.producto.ProductoRepositories.ProductoRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas RN-005 / REQ-F-006..008: el inventario se lleva en unidad base; toda cantidad en
 * otra unidad se multiplica por su factor exacto. No hay redondeos silenciosos: si la cantidad
 * convertida requiere más de 4 decimales la operación se rechaza.
 */
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ConversionUnidadRepository conversionRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadRepository;

    @Value("${sigif.inventario.decimales-maximos-venta:3}")
    private int decimalesMaximos = 3;

    /** Propósito de la conversión. */
    public enum Uso {
        /** Entrada por compra. */
        COMPRA,
        /** Salida por venta o reserva. */
        VENTA
    }

    /**
     * Resultado de convertir una cantidad.
     *
     * @param unidad       unidad usada en el documento
     * @param factor       factor aplicado
     * @param cantidadBase cantidad en unidad base (4 decimales)
     */
    public record Conversion(UnidadMedida unidad, BigDecimal factor, BigDecimal cantidadBase) {
    }

    /**
     * @param unidadId    unidad alterna
     * @param factor      unidades base por unidad alterna
     * @param usoCompra   habilitada en compras
     * @param usoVenta    habilitada en ventas
     */
    public record ConversionRequest(@NotNull(message = "{validacion.obligatorio}") Long unidadId,
                                    @NotNull(message = "{validacion.obligatorio}")
                                    @DecimalMin(value = "0.000001", message = "{validacion.positivo}")
                                    @Digits(integer = 12, fraction = 6, message = "{validacion.decimales}")
                                    BigDecimal factor,
                                    Boolean usoCompra, Boolean usoVenta) {
    }

    /**
     * @param id         id
     * @param unidadId   unidad
     * @param unidadCodigo código
     * @param unidadNombre nombre
     * @param factor     factor
     * @param usoCompra  uso en compras
     * @param usoVenta   uso en ventas
     */
    public record ConversionResponse(Long id, Long unidadId, String unidadCodigo, String unidadNombre,
                                     BigDecimal factor, boolean usoCompra, boolean usoVenta) {

        /**
         * @param c entidad
         * @return DTO
         */
        public static ConversionResponse de(ConversionUnidad c) {
            return new ConversionResponse(c.getId(), c.getUnidad().getId(), c.getUnidad().getCodigo(),
                    c.getUnidad().getNombre(), c.getFactor(), c.isUsoCompra(), c.isUsoVenta());
        }
    }

    /**
     * Convierte una cantidad expresada en {@code unidadId} a unidad base y valida fracciones.
     *
     * @param producto producto
     * @param unidadId unidad del documento (null = unidad base)
     * @param cantidad cantidad en esa unidad
     * @param uso      compra o venta
     * @return conversión con cantidad base exacta
     */
    @Transactional(readOnly = true)
    public Conversion convertir(Producto producto, Long unidadId, BigDecimal cantidad, Uso uso) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioException("cantidad.invalida");
        }
        if (Numeros.decimalesSignificativos(cantidad) > decimalesMaximos) {
            throw new ReglaNegocioException("cantidad.decimales", decimalesMaximos);
        }
        UnidadMedida unidad = producto.getUnidadBase();
        BigDecimal factor = BigDecimal.ONE;
        if (unidadId != null && !unidadId.equals(unidad.getId())) {
            ConversionUnidad conv = conversionRepository.findByProductoIdAndUnidadId(producto.getId(), unidadId)
                    .orElseThrow(() -> new ReglaNegocioException("conversion.no.configurada", producto.getNombre()));
            if ((uso == Uso.VENTA && !conv.isUsoVenta()) || (uso == Uso.COMPRA && !conv.isUsoCompra())) {
                throw new ReglaNegocioException("conversion.no.habilitada", producto.getNombre());
            }
            unidad = conv.getUnidad();
            factor = conv.getFactor();
        }
        BigDecimal base = cantidad.multiply(factor);
        if (Numeros.decimalesSignificativos(base) > Numeros.ESCALA_CANTIDAD) {
            throw new ReglaNegocioException("cantidad.precision", producto.getNombre());
        }
        base = base.setScale(Numeros.ESCALA_CANTIDAD, RoundingMode.UNNECESSARY);
        if (!producto.isPermiteFraccion() && !Numeros.esEntero(base)) {
            throw new ReglaNegocioException("producto.no.fraccionable", producto.getNombre());
        }
        return new Conversion(unidad, factor, base);
    }

    /**
     * @param productoId producto
     * @return conversiones configuradas
     */
    @Transactional(readOnly = true)
    public List<ConversionResponse> listar(Long productoId) {
        return conversionRepository.findByProductoIdOrderByFactorAsc(productoId).stream()
                .map(ConversionResponse::de).toList();
    }

    /**
     * @param productoId producto
     * @param req        conversión
     * @return conversión creada
     */
    @Transactional
    public ConversionResponse agregar(Long productoId, ConversionRequest req) {
        Producto p = productoRepository.findWithRelacionesById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.producto", productoId));
        if (p.getUnidadBase().getId().equals(req.unidadId())) {
            throw new ReglaNegocioException("conversion.unidad.base");
        }
        if (conversionRepository.findByProductoIdAndUnidadId(productoId, req.unidadId()).isPresent()) {
            throw new ConflictoException("conversion.duplicada");
        }
        ConversionUnidad c = new ConversionUnidad();
        c.setProducto(p);
        c.setUnidad(unidadRepository.findById(req.unidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.unidad", req.unidadId())));
        c.setFactor(req.factor());
        c.setUsoCompra(req.usoCompra() == null || req.usoCompra());
        c.setUsoVenta(req.usoVenta() == null || req.usoVenta());
        return ConversionResponse.de(conversionRepository.save(c));
    }

    /**
     * @param productoId   producto
     * @param conversionId conversión
     */
    @Transactional
    public void eliminar(Long productoId, Long conversionId) {
        ConversionUnidad c = conversionRepository.findById(conversionId)
                .filter(x -> x.getProducto().getId().equals(productoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.conversion", conversionId));
        conversionRepository.delete(c);
    }
}
