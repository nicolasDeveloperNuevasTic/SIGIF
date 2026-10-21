/*
 * Nombre: CierreService.java
 * Descripción: Ejecución y consulta de cierres mensuales de inventario con consolidación por estado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.cierre;

import com.sigif.cierre.CierreMensual.TipoEjecucion;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.common.util.Numeros;
import com.sigif.common.util.UsuarioActual;
import com.sigif.producto.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierre mensual (REQ-F-027 a REQ-F-029). El saldo final de cada estado se reconstruye desde el
 * saldo actual restando los movimientos posteriores al periodo, y el inicial restando además
 * los del periodo; así el cierre es exacto aunque se ejecute días después del corte.
 */
@Service
@RequiredArgsConstructor
public class CierreService {

    private static final String SQL_DELTAS = """
            SELECT producto_id,
                   COALESCE(SUM(CASE WHEN tipo = 'ENTRADA_COMPRA' THEN cantidad END), 0) AS compras,
                   COALESCE(SUM(CASE WHEN tipo = 'SALIDA_VENTA' THEN cantidad END), 0) AS ventas,
                   COALESCE(SUM(CASE WHEN estado_destino = 'DISPONIBLE' THEN cantidad ELSE 0 END)
                          - SUM(CASE WHEN estado_origen = 'DISPONIBLE' THEN cantidad ELSE 0 END), 0) AS d_disp,
                   COALESCE(SUM(CASE WHEN estado_destino = 'RESERVADA' THEN cantidad ELSE 0 END)
                          - SUM(CASE WHEN estado_origen = 'RESERVADA' THEN cantidad ELSE 0 END), 0) AS d_res,
                   COALESCE(SUM(CASE WHEN estado_destino = 'DANADA' THEN cantidad ELSE 0 END)
                          - SUM(CASE WHEN estado_origen = 'DANADA' THEN cantidad ELSE 0 END), 0) AS d_dan
              FROM movimiento_inventario
             WHERE %s
             GROUP BY producto_id
            """;

    private final CierreMensualRepository repository;
    private final PeriodoService periodoService;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param periodo AAAAMM
     * @param forzar  cerrar aunque existan conflictos de sincronización
     */
    public record CierreRequest(@NotNull(message = "{validacion.obligatorio}") Integer periodo, boolean forzar) {
    }

    /**
     * @param periodo                  periodo evaluado
     * @param cerrado                  ya está cerrado
     * @param finalizado               el mes ya terminó
     * @param conflictosSincronizacion operaciones offline en conflicto
     * @param reservasActivas          reservas activas que pasarán al siguiente mes
     * @param alertas                  claves de alerta
     */
    public record PrevalidacionResponse(int periodo, boolean cerrado, boolean finalizado,
                                        long conflictosSincronizacion, long reservasActivas, List<String> alertas) {
    }

    /**
     * @param id                     id
     * @param periodo                periodo
     * @param fechaEjecucion         fecha
     * @param tipoEjecucion          PROGRAMADO / MANUAL
     * @param operacionesPendientes  conflictos al cierre
     * @param valorInventario        valor total
     * @param observaciones          observaciones
     * @param usuario                usuario
     * @param detalles               consolidado
     */
    public record CierreResponse(Long id, Integer periodo, OffsetDateTime fechaEjecucion, String tipoEjecucion,
                                 int operacionesPendientes, BigDecimal valorInventario, String observaciones,
                                 String usuario, List<DetalleResponse> detalles) {
    }

    /**
     * @param productoId        producto
     * @param codigoBarras      código
     * @param producto          nombre
     * @param inicialDisponible inicial disponible
     * @param inicialReservada  inicial reservada
     * @param inicialDanada     inicial dañada
     * @param compras           compras del mes
     * @param ventas            ventas del mes
     * @param finalDisponible   final disponible
     * @param finalReservada    final reservada
     * @param finalDanada       final dañada
     * @param costoPromedio     costo
     * @param valorInventario   valor
     */
    public record DetalleResponse(Long productoId, String codigoBarras, String producto, BigDecimal inicialDisponible,
                                  BigDecimal inicialReservada, BigDecimal inicialDanada, BigDecimal compras,
                                  BigDecimal ventas, BigDecimal finalDisponible, BigDecimal finalReservada,
                                  BigDecimal finalDanada, BigDecimal costoPromedio, BigDecimal valorInventario) {
    }

    /**
     * @param periodo AAAAMM
     * @return condiciones previas al cierre (REQ-F-029)
     */
    @Transactional(readOnly = true)
    public PrevalidacionResponse prevalidar(int periodo) {
        validarFormato(periodo);
        long conflictos = conflictosSincronizacion();
        Long reservas = jdbcTemplate.queryForObject("SELECT count(*) FROM reserva WHERE estado = 'ACTIVA'", Long.class);
        boolean cerrado = repository.existsByPeriodo(periodo);
        boolean finalizado = periodo < periodoService.periodoActual();
        List<String> alertas = new ArrayList<>();
        if (cerrado) {
            alertas.add("cierre.ya.existe");
        }
        if (!finalizado) {
            alertas.add("cierre.periodo.no.finalizado");
        }
        if (conflictos > 0) {
            alertas.add("cierre.pendientes.sincronizacion");
        }
        if (reservas != null && reservas > 0) {
            alertas.add("cierre.reservas.activas");
        }
        return new PrevalidacionResponse(periodo, cerrado, finalizado, conflictos, reservas == null ? 0 : reservas,
                alertas);
    }

    /**
     * Ejecuta el cierre del periodo.
     *
     * @param periodo AAAAMM
     * @param tipo    programado o manual
     * @param forzar  ignorar conflictos de sincronización (solo manual)
     * @return cierre generado
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CierreResponse ejecutar(int periodo, TipoEjecucion tipo, boolean forzar) {
        validarFormato(periodo);
        if (periodo >= periodoService.periodoActual()) {
            throw new ReglaNegocioException("cierre.periodo.no.finalizado");
        }
        if (repository.existsByPeriodo(periodo)) {
            throw new ConflictoException("cierre.ya.existe", periodo);
        }
        repository.ultimoPeriodoCerrado().filter(u -> periodo < u).ifPresent(u -> {
            throw new ConflictoException("cierre.periodo.anterior", u);
        });
        long conflictos = conflictosSincronizacion();
        if (conflictos > 0 && tipo == TipoEjecucion.MANUAL && !forzar) {
            throw new ConflictoException("cierre.pendientes.sincronizacion", conflictos);
        }

        Map<Long, BigDecimal[]> posteriores = deltas("periodo > ?", periodo);
        Map<Long, BigDecimal[]> delPeriodo = deltas("periodo = ?", periodo);

        CierreMensual cierre = new CierreMensual();
        cierre.setPeriodo(periodo);
        cierre.setFechaEjecucion(OffsetDateTime.now());
        cierre.setTipoEjecucion(tipo);
        cierre.setOperacionesPendientes((int) conflictos);
        cierre.setUsuario(UsuarioActual.nombre());
        cierre.setObservaciones(conflictos > 0
                ? "ALERTA: " + conflictos + " operaciones offline en conflicto al momento del corte" : null);

        BigDecimal valorTotal = BigDecimal.ZERO;
        List<Map<String, Object>> saldos = jdbcTemplate.queryForList("""
                SELECT s.producto_id, s.cantidad_disponible, s.cantidad_reservada, s.cantidad_danada, p.precio_compra
                  FROM inventario_saldo s JOIN producto p ON p.id = s.producto_id ORDER BY s.producto_id
                """);
        BigDecimal[] cero = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (Map<String, Object> fila : saldos) {
            Long productoId = ((Number) fila.get("producto_id")).longValue();
            BigDecimal[] post = posteriores.getOrDefault(productoId, cero);
            BigDecimal[] mes = delPeriodo.getOrDefault(productoId, cero);
            BigDecimal finD = ((BigDecimal) fila.get("cantidad_disponible")).subtract(post[2]);
            BigDecimal finR = ((BigDecimal) fila.get("cantidad_reservada")).subtract(post[3]);
            BigDecimal finDan = ((BigDecimal) fila.get("cantidad_danada")).subtract(post[4]);
            boolean sinActividad = finD.signum() == 0 && finR.signum() == 0 && finDan.signum() == 0
                    && mes[0].signum() == 0 && mes[1].signum() == 0;
            if (sinActividad) {
                continue;
            }
            BigDecimal costo = (BigDecimal) fila.get("precio_compra");
            CierreMensualDetalle d = new CierreMensualDetalle();
            d.setCierre(cierre);
            d.setProducto(entityManager.getReference(Producto.class, productoId));
            d.setFinalDisponible(finD);
            d.setFinalReservada(finR);
            d.setFinalDanada(finDan);
            d.setInicialDisponible(finD.subtract(mes[2]));
            d.setInicialReservada(finR.subtract(mes[3]));
            d.setInicialDanada(finDan.subtract(mes[4]));
            d.setCompras(mes[0]);
            d.setVentas(mes[1]);
            d.setCostoPromedio(costo);
            d.setValorInventario(Numeros.moneda(finD.add(finR).add(finDan).multiply(costo)));
            valorTotal = valorTotal.add(d.getValorInventario());
            cierre.getDetalles().add(d);
        }
        cierre.setValorInventario(Numeros.moneda(valorTotal));
        repository.save(cierre);
        return respuesta(cierre, cierre.getDetalles());
    }

    /**
     * @return cierres registrados (sin detalle)
     */
    @Transactional(readOnly = true)
    public List<CierreResponse> listar() {
        return repository.findAllByOrderByPeriodoDesc().stream().map(c -> respuesta(c, List.of())).toList();
    }

    /**
     * @param id id
     * @return cierre con detalle
     */
    @Transactional(readOnly = true)
    public CierreResponse obtener(Long id) {
        CierreMensual c = repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("recurso.cierre", id));
        return respuesta(c, repository.detalles(id));
    }

    /**
     * @param periodo periodo
     * @return true si existe cierre
     */
    @Transactional(readOnly = true)
    public boolean existe(int periodo) {
        return repository.existsByPeriodo(periodo);
    }

    private long conflictosSincronizacion() {
        Long n = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sincronizacion_operacion WHERE estado = 'CONFLICTO'", Long.class);
        return n == null ? 0 : n;
    }

    private Map<Long, BigDecimal[]> deltas(String condicion, int periodo) {
        Map<Long, BigDecimal[]> mapa = new HashMap<>();
        jdbcTemplate.query(String.format(SQL_DELTAS, condicion), rs -> {
            mapa.put(rs.getLong("producto_id"), new BigDecimal[]{rs.getBigDecimal("compras"), rs.getBigDecimal("ventas"),
                    rs.getBigDecimal("d_disp"), rs.getBigDecimal("d_res"), rs.getBigDecimal("d_dan")});
        }, periodo);
        return mapa;
    }

    private static void validarFormato(int periodo) {
        int mes = periodo % 100;
        if (periodo < 200001 || periodo > 299912 || mes < 1 || mes > 12) {
            throw new ReglaNegocioException("cierre.periodo.invalido");
        }
    }

    private CierreResponse respuesta(CierreMensual c, List<CierreMensualDetalle> detalles) {
        return new CierreResponse(c.getId(), c.getPeriodo(), c.getFechaEjecucion(), c.getTipoEjecucion().name(),
                c.getOperacionesPendientes(), c.getValorInventario(), c.getObservaciones(), c.getUsuario(),
                detalles.stream().map(d -> new DetalleResponse(d.getProducto().getId(), d.getProducto().getCodigoBarras(),
                        d.getProducto().getNombre(), d.getInicialDisponible(), d.getInicialReservada(),
                        d.getInicialDanada(), d.getCompras(), d.getVentas(), d.getFinalDisponible(),
                        d.getFinalReservada(), d.getFinalDanada(), d.getCostoPromedio(), d.getValorInventario()))
                        .toList());
    }
}
