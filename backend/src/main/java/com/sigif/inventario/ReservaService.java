/*
 * Nombre: ReservaService.java
 * Descripción: Creación, cancelación, consumo y expiración automática de reservas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.util.UsuarioActual;
import com.sigif.inventario.InventarioDtos.ReservaRequest;
import com.sigif.inventario.InventarioDtos.ReservaResponse;
import com.sigif.inventario.InventarioRepositories.ReservaRepository;
import com.sigif.producto.Producto;
import com.sigif.producto.ProductoService;
import com.sigif.tercero.Tercero;
import com.sigif.tercero.TerceroService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Una reserva mueve unidades de DISPONIBLE a RESERVADA para un cliente. Solo ese cliente puede
 * comprarlas (RN-007); al vencer, el proceso programado las devuelve a DISPONIBLE.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final InventarioService inventarioService;
    private final ProductoService productoService;
    private final TerceroService terceroService;

    @Value("${sigif.reservas.dias-vigencia-defecto:3}")
    private int diasDefecto = 3;

    /**
     * @param req reserva
     * @return reserva creada
     */
    @Transactional
    public ReservaResponse crear(ReservaRequest req) {
        Producto p = productoService.entidad(req.productoId());
        if (!p.esActivo()) {
            throw new ReglaNegocioException("producto.inactivo", p.getNombre());
        }
        if (!p.isPermiteFraccion() && req.cantidad().stripTrailingZeros().scale() > 0) {
            throw new ReglaNegocioException("producto.no.fraccionable", p.getNombre());
        }
        Tercero cliente = terceroService.clienteValido(req.clienteId());
        if (cliente.isEsGenerico()) {
            throw new ReglaNegocioException("reserva.cliente.generico");
        }
        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setProducto(p);
        r.setCantidad(req.cantidad());
        r.setCantidadPendiente(req.cantidad());
        r.setFechaExpiracion(OffsetDateTime.now().plusDays(req.diasVigencia() == null ? diasDefecto : req.diasVigencia()));
        r.setObservaciones(req.observaciones());
        r.setUsuario(UsuarioActual.nombre());
        reservaRepository.save(r);
        inventarioService.cambiarEstado(p, req.cantidad(), EstadoInventario.DISPONIBLE, EstadoInventario.RESERVADA,
                "Reserva #" + r.getId() + " - " + cliente.nombreMostrar(), r);
        return ReservaResponse.de(r);
    }

    /**
     * @param id id
     * @return reserva cancelada
     */
    @Transactional
    public ReservaResponse cancelar(Long id) {
        Reserva r = reservaRepository.bloquear(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.reserva", id));
        liberar(r, Reserva.Estado.CANCELADA, "Cancelación de reserva #" + id);
        return ReservaResponse.de(r);
    }

    /**
     * Valida y descuenta la reserva al venderse (la salida de inventario la registra la venta).
     *
     * @param reservaId    reserva
     * @param clienteId    cliente de la factura
     * @param productoId   producto vendido
     * @param cantidadBase cantidad en unidad base
     * @return reserva actualizada
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reserva consumir(Long reservaId, Long clienteId, Long productoId, BigDecimal cantidadBase) {
        Reserva r = reservaRepository.bloquear(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.reserva", reservaId));
        if (r.getEstado() != Reserva.Estado.ACTIVA) {
            throw new ConflictoException("reserva.no.activa", reservaId);
        }
        if (!r.getCliente().getId().equals(clienteId)) {
            throw new ConflictoException("reserva.otro.cliente", reservaId);
        }
        if (!r.getProducto().getId().equals(productoId)) {
            throw new ReglaNegocioException("reserva.otro.producto", reservaId);
        }
        if (r.getCantidadPendiente().compareTo(cantidadBase) < 0) {
            throw new ConflictoException("reserva.cantidad.insuficiente", reservaId,
                    r.getCantidadPendiente().stripTrailingZeros().toPlainString());
        }
        r.setCantidadPendiente(r.getCantidadPendiente().subtract(cantidadBase));
        if (r.getCantidadPendiente().signum() == 0) {
            r.setEstado(Reserva.Estado.CONSUMIDA);
        }
        return r;
    }

    /**
     * Expira una reserva vencida y devuelve el saldo pendiente a DISPONIBLE.
     *
     * @param id id de la reserva
     */
    @Transactional
    public void expirar(Long id) {
        reservaRepository.bloquear(id)
                .filter(r -> r.getEstado() == Reserva.Estado.ACTIVA && r.getFechaExpiracion().isBefore(OffsetDateTime.now()))
                .ifPresent(r -> liberar(r, Reserva.Estado.EXPIRADA, "Expiración automática de reserva #" + id));
    }

    /**
     * @return ids de reservas activas vencidas
     */
    @Transactional(readOnly = true)
    public List<Long> vencidas() {
        return reservaRepository.idsVencidas(Reserva.Estado.ACTIVA, OffsetDateTime.now());
    }

    /**
     * @param estado     filtro por estado
     * @param clienteId  filtro por cliente
     * @param productoId filtro por producto
     * @param pagina     página
     * @param tamano     tamaño
     * @return página de reservas
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> buscar(Reserva.Estado estado, Long clienteId, Long productoId,
                                                  int pagina, int tamano) {
        Specification<Reserva> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (productoId != null) {
                p.add(cb.equal(root.get("producto").get("id"), productoId));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(reservaRepository.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 200),
                Sort.by(Sort.Direction.DESC, "id"))), ReservaResponse::de);
    }

    private void liberar(Reserva r, Reserva.Estado nuevoEstado, String motivo) {
        if (r.getEstado() != Reserva.Estado.ACTIVA) {
            throw new ConflictoException("reserva.no.activa", r.getId());
        }
        if (r.getCantidadPendiente().signum() > 0) {
            inventarioService.cambiarEstado(r.getProducto(), r.getCantidadPendiente(), EstadoInventario.RESERVADA,
                    EstadoInventario.DISPONIBLE, motivo, r);
        }
        r.setEstado(nuevoEstado);
    }
}
