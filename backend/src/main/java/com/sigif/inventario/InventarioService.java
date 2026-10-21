/*
 * Nombre: InventarioService.java
 * Descripción: Núcleo transaccional del inventario en bodega única: entradas, salidas, cambios
 *              de estado, control FEFO de vencimientos y prohibición de saldos negativos.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.cierre.PeriodoService;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.util.UsuarioActual;
import com.sigif.compra.DocumentoIngreso;
import com.sigif.inventario.InventarioRepositories.InventarioSaldoRepository;
import com.sigif.inventario.InventarioRepositories.LoteRepository;
import com.sigif.inventario.InventarioRepositories.MovimientoInventarioRepository;
import com.sigif.inventario.MovimientoInventario.Tipo;
import com.sigif.producto.Producto;
import com.sigif.venta.Factura;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Todas las operaciones bloquean la fila de saldo del producto ({@code SELECT ... FOR UPDATE})
 * antes de validarla, por lo que ventas simultáneas se serializan y nunca generan saldos
 * negativos (REQ-F-015, REQ-F-024). Cada operación deja un movimiento inmutable (REQ-F-033).
 */
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioSaldoRepository saldoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PeriodoService periodoService;

    /**
     * Crea el registro de saldo en cero para un producto nuevo.
     *
     * @param productoId producto
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void inicializarSaldo(Long productoId) {
        if (!saldoRepository.existsById(productoId)) {
            InventarioSaldo s = new InventarioSaldo();
            s.setProductoId(productoId);
            saldoRepository.save(s);
        }
    }

    /**
     * @param productoId producto
     * @return saldo bloqueado para escritura
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventarioSaldo bloquear(Long productoId) {
        return saldoRepository.bloquear(productoId).orElseGet(() -> {
            inicializarSaldo(productoId);
            saldoRepository.flush();
            return saldoRepository.bloquear(productoId).orElseThrow();
        });
    }

    /**
     * Entrada por compra al estado DISPONIBLE (REQ-F-018).
     *
     * @param saldo         saldo ya bloqueado
     * @param producto      producto
     * @param cantidadBase  cantidad en unidad base
     * @param costoUnitario costo por unidad base
     * @param documento     documento de ingreso
     * @param lote          lote (productos con vencimiento) o null
     * @param fecha         fecha del documento
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEntrada(InventarioSaldo saldo, Producto producto, BigDecimal cantidadBase,
                                 BigDecimal costoUnitario, DocumentoIngreso documento, Lote lote, OffsetDateTime fecha) {
        saldo.ajustar(EstadoInventario.DISPONIBLE, cantidadBase, producto.getNombre());
        MovimientoInventario m = nuevo(Tipo.ENTRADA_COMPRA, producto, cantidadBase, fecha);
        m.setEstadoDestino(EstadoInventario.DISPONIBLE);
        m.setCostoUnitario(costoUnitario);
        m.setDocumentoIngreso(documento);
        m.setLote(lote);
        movimientoRepository.save(m);
    }

    /**
     * Salida por venta desde DISPONIBLE o RESERVADA (REQ-F-021). Nunca desde DANADA (RN-007).
     * Para productos con vencimiento consume lotes vigentes en orden FEFO y bloquea la venta si
     * solo quedan unidades vencidas (REQ-F-016).
     *
     * @param producto     producto
     * @param cantidadBase cantidad en unidad base
     * @param origen       DISPONIBLE o RESERVADA
     * @param factura      factura
     * @param reserva      reserva consumida o null
     * @param fecha        fecha de la factura
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarSalida(Producto producto, BigDecimal cantidadBase, EstadoInventario origen, Factura factura,
                                Reserva reserva, OffsetDateTime fecha) {
        if (origen == EstadoInventario.DANADA) {
            throw new ConflictoException("inventario.venta.danada", producto.getNombre());
        }
        InventarioSaldo saldo = bloquear(producto.getId());
        saldo.ajustar(origen, cantidadBase.negate(), producto.getNombre());

        if (!producto.isTieneVencimiento()) {
            MovimientoInventario m = nuevo(Tipo.SALIDA_VENTA, producto, cantidadBase, fecha);
            m.setEstadoOrigen(origen);
            m.setFactura(factura);
            m.setReserva(reserva);
            m.setCostoUnitario(producto.getPrecioCompra());
            movimientoRepository.save(m);
            return;
        }
        LocalDate hoy = fecha.atZoneSameInstant(periodoService.zona()).toLocalDate();
        List<Lote> lotes = loteRepository.bloquearVigentes(producto.getId(), hoy);
        BigDecimal vigente = lotes.stream().map(Lote::getCantidadActual).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (vigente.compareTo(cantidadBase) < 0) {
            throw new ConflictoException("inventario.vencido", producto.getNombre(),
                    vigente.stripTrailingZeros().toPlainString());
        }
        BigDecimal pendiente = cantidadBase;
        for (Lote lote : lotes) {
            if (pendiente.signum() == 0) {
                break;
            }
            BigDecimal tomar = lote.getCantidadActual().min(pendiente);
            lote.setCantidadActual(lote.getCantidadActual().subtract(tomar));
            pendiente = pendiente.subtract(tomar);
            MovimientoInventario m = nuevo(Tipo.SALIDA_VENTA, producto, tomar, fecha);
            m.setEstadoOrigen(origen);
            m.setFactura(factura);
            m.setReserva(reserva);
            m.setLote(lote);
            m.setCostoUnitario(producto.getPrecioCompra());
            movimientoRepository.save(m);
        }
    }

    /**
     * Transición entre estados con usuario, fecha y motivo (REQ-F-014).
     *
     * @param producto producto
     * @param cantidad cantidad en unidad base
     * @param origen   estado origen
     * @param destino  estado destino
     * @param motivo   motivo obligatorio
     * @param reserva  reserva asociada o null
     * @return saldo resultante
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventarioSaldo cambiarEstado(Producto producto, BigDecimal cantidad, EstadoInventario origen,
                                         EstadoInventario destino, String motivo, Reserva reserva) {
        if (origen == destino) {
            throw new ReglaNegocioException("inventario.transicion.mismo.estado");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioException("cantidad.invalida");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("inventario.transicion.motivo");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        periodoService.validarAbierto(periodoService.periodo(ahora));
        InventarioSaldo saldo = bloquear(producto.getId());
        saldo.ajustar(origen, cantidad.negate(), producto.getNombre());
        saldo.ajustar(destino, cantidad, producto.getNombre());
        MovimientoInventario m = nuevo(Tipo.CAMBIO_ESTADO, producto, cantidad, ahora);
        m.setEstadoOrigen(origen);
        m.setEstadoDestino(destino);
        m.setMotivo(motivo.length() > 300 ? motivo.substring(0, 300) : motivo);
        m.setReserva(reserva);
        movimientoRepository.save(m);
        return saldo;
    }

    private MovimientoInventario nuevo(Tipo tipo, Producto producto, BigDecimal cantidad, OffsetDateTime fecha) {
        MovimientoInventario m = new MovimientoInventario();
        m.setTipo(tipo);
        m.setProducto(producto);
        m.setCantidad(cantidad);
        m.setFecha(fecha);
        m.setPeriodo(periodoService.periodo(fecha));
        m.setUsuario(UsuarioActual.nombre());
        return m;
    }
}
