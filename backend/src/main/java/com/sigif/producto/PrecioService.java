/*
 * Nombre: PrecioService.java
 * Descripción: Cálculo del precio de venta por incremento porcentual, costo promedio e historial.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.util.Numeros;
import com.sigif.common.util.UsuarioActual;
import com.sigif.producto.ProductoRepositories.HistorialPrecioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas RN-013 / REQ-F-009..011: {@code Precio Venta = Precio Compra * (1 + Porcentaje / 100)}.
 * El precio de compra del producto es el costo promedio ponderado de sus existencias.
 */
@Service
@RequiredArgsConstructor
public class PrecioService {

    private final HistorialPrecioRepository historialRepository;

    /**
     * @param precioCompra costo unitario (unidad base)
     * @param porcentaje   porcentaje de incremento
     * @return precio de venta con 4 decimales
     */
    public static BigDecimal calcularPrecioVenta(BigDecimal precioCompra, BigDecimal porcentaje) {
        BigDecimal factor = BigDecimal.ONE.add(porcentaje.divide(Numeros.CIEN, 10, RoundingMode.HALF_UP));
        return Numeros.precio(precioCompra.multiply(factor));
    }

    /**
     * Costo promedio ponderado tras una entrada.
     *
     * @param existencia  existencias totales antes de la entrada (unidad base)
     * @param costoActual costo promedio actual
     * @param cantidad    cantidad que ingresa (unidad base)
     * @param costoNuevo  costo unitario de la entrada (unidad base)
     * @return nuevo costo promedio con 4 decimales
     */
    public static BigDecimal costoPromedio(BigDecimal existencia, BigDecimal costoActual, BigDecimal cantidad,
                                           BigDecimal costoNuevo) {
        BigDecimal total = existencia.add(cantidad);
        if (existencia.signum() <= 0 || total.signum() <= 0) {
            return Numeros.precio(costoNuevo);
        }
        BigDecimal valor = existencia.multiply(costoActual).add(cantidad.multiply(costoNuevo));
        return valor.divide(total, Numeros.ESCALA_PRECIO, RoundingMode.HALF_UP);
    }

    /**
     * Valida costo y porcentaje (REQ-F-011).
     *
     * @param precioCompra costo
     * @param porcentaje   porcentaje
     */
    public static void validar(BigDecimal precioCompra, BigDecimal porcentaje) {
        if (precioCompra == null || precioCompra.signum() <= 0) {
            throw new ReglaNegocioException("precio.compra.invalido");
        }
        if (porcentaje == null || porcentaje.signum() < 0) {
            throw new ReglaNegocioException("precio.porcentaje.invalido");
        }
    }

    /**
     * Aplica nuevo costo y/o porcentaje al producto, recalcula el precio de venta y registra el
     * historial si hubo cambio. El producto debe estar persistido.
     *
     * @param producto     producto gestionado
     * @param precioCompra nuevo costo
     * @param porcentaje   nuevo porcentaje
     * @param motivo       motivo del cambio
     * @param referencia   documento relacionado (ej. número de documento de ingreso)
     */
    @Transactional
    public void aplicar(Producto producto, BigDecimal precioCompra, BigDecimal porcentaje,
                        HistorialPrecio.Motivo motivo, String referencia) {
        validar(precioCompra, porcentaje);
        BigDecimal compra = Numeros.precio(precioCompra);
        BigDecimal pct = porcentaje.setScale(2, RoundingMode.HALF_UP);
        BigDecimal venta = calcularPrecioVenta(compra, pct);
        boolean sinCambio = motivo != HistorialPrecio.Motivo.CREACION
                && igual(producto.getPrecioCompra(), compra)
                && igual(producto.getPorcentajeIncremento(), pct);
        if (sinCambio) {
            return;
        }
        HistorialPrecio h = new HistorialPrecio();
        h.setProducto(producto);
        h.setPrecioCompraAnterior(motivo == HistorialPrecio.Motivo.CREACION ? null : producto.getPrecioCompra());
        h.setPorcentajeAnterior(motivo == HistorialPrecio.Motivo.CREACION ? null : producto.getPorcentajeIncremento());
        h.setPrecioVentaAnterior(motivo == HistorialPrecio.Motivo.CREACION ? null : producto.getPrecioVenta());
        h.setPrecioCompraNuevo(compra);
        h.setPorcentajeNuevo(pct);
        h.setPrecioVentaNuevo(venta);
        h.setMotivo(motivo);
        h.setReferencia(referencia);
        h.setUsuario(UsuarioActual.nombre());
        h.setFecha(OffsetDateTime.now());
        producto.setPrecioCompra(compra);
        producto.setPorcentajeIncremento(pct);
        producto.setPrecioVenta(venta);
        historialRepository.save(h);
    }

    private static boolean igual(BigDecimal a, BigDecimal b) {
        return Objects.equals(a == null ? null : a.stripTrailingZeros(), b == null ? null : b.stripTrailingZeros());
    }
}
