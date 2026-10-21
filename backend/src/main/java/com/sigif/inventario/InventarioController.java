/*
 * Nombre: InventarioController.java
 * Descripción: Endpoints REST de existencias, kardex, lotes, alertas, transiciones y reservas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.inventario.InventarioDtos.LoteResponse;
import com.sigif.inventario.InventarioDtos.MovimientoResponse;
import com.sigif.inventario.InventarioDtos.ReservaRequest;
import com.sigif.inventario.InventarioDtos.ReservaResponse;
import com.sigif.inventario.InventarioDtos.SaldoResponse;
import com.sigif.inventario.InventarioDtos.TransicionRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas /api/v1/inventario y /api/v1/reservas.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioConsultaService consultaService;
    private final ReservaService reservaService;

    /**
     * @param q            texto
     * @param soloConStock solo con existencias
     * @param pagina       página
     * @param tamano       tamaño
     * @return saldos por estado
     */
    @GetMapping("/inventario/saldos")
    @PreAuthorize("hasAuthority('INVENTARIO_LEER')")
    public PaginaResponse<SaldoResponse> saldos(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "false") boolean soloConStock,
                                                @RequestParam(defaultValue = "0") int pagina,
                                                @RequestParam(defaultValue = "20") int tamano) {
        return consultaService.saldos(q, soloConStock, pagina, tamano);
    }

    /**
     * @param productoId producto
     * @param tipo       tipo
     * @param desde      desde
     * @param hasta      hasta
     * @param pagina     página
     * @param tamano     tamaño
     * @return kardex
     */
    @GetMapping("/inventario/movimientos")
    @PreAuthorize("hasAuthority('INVENTARIO_LEER')")
    public PaginaResponse<MovimientoResponse> movimientos(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) MovimientoInventario.Tipo tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return consultaService.movimientos(productoId, tipo, desde, hasta, pagina, tamano);
    }

    /**
     * @param productoId producto
     * @return lotes
     */
    @GetMapping("/inventario/lotes")
    @PreAuthorize("hasAuthority('INVENTARIO_LEER')")
    public List<LoteResponse> lotes(@RequestParam Long productoId) {
        return consultaService.lotes(productoId);
    }

    /**
     * @param dias horizonte
     * @return lotes vencidos o por vencer
     */
    @GetMapping("/inventario/alertas/vencimientos")
    @PreAuthorize("hasAuthority('INVENTARIO_LEER')")
    public List<LoteResponse> vencimientos(@RequestParam(defaultValue = "30") int dias) {
        return consultaService.alertasVencimiento(dias);
    }

    /**
     * @return productos bajo stock mínimo
     */
    @GetMapping("/inventario/alertas/stock-minimo")
    @PreAuthorize("hasAuthority('INVENTARIO_LEER')")
    public List<SaldoResponse> stockMinimo() {
        return consultaService.alertasStockMinimo();
    }

    /**
     * @param req transición
     * @return saldo resultante
     */
    @PostMapping("/inventario/transiciones")
    @PreAuthorize("hasAuthority('INVENTARIO_TRANSICION')")
    public SaldoResponse transicion(@Valid @RequestBody TransicionRequest req) {
        return consultaService.transicion(req);
    }

    /**
     * @param estado     estado
     * @param clienteId  cliente
     * @param productoId producto
     * @param pagina     página
     * @param tamano     tamaño
     * @return reservas
     */
    @GetMapping("/reservas")
    @PreAuthorize("hasAuthority('INVENTARIO_LEER')")
    public PaginaResponse<ReservaResponse> reservas(@RequestParam(required = false) Reserva.Estado estado,
                                                    @RequestParam(required = false) Long clienteId,
                                                    @RequestParam(required = false) Long productoId,
                                                    @RequestParam(defaultValue = "0") int pagina,
                                                    @RequestParam(defaultValue = "20") int tamano) {
        return reservaService.buscar(estado, clienteId, productoId, pagina, tamano);
    }

    /**
     * @param req reserva
     * @return reserva creada
     */
    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RESERVA_GESTIONAR')")
    public ReservaResponse crearReserva(@Valid @RequestBody ReservaRequest req) {
        return reservaService.crear(req);
    }

    /**
     * @param id reserva
     * @return reserva cancelada
     */
    @PostMapping("/reservas/{id}/cancelar")
    @PreAuthorize("hasAuthority('RESERVA_GESTIONAR')")
    public ReservaResponse cancelarReserva(@PathVariable Long id) {
        return reservaService.cancelar(id);
    }
}
