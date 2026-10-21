/*
 * Nombre: ReporteController.java
 * Descripción: Tablero de indicadores operativos del inventario y las ventas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.reporte;

import com.sigif.cierre.PeriodoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/v1/reportes/resumen: cifras del tablero principal del Front-End.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final JdbcTemplate jdbcTemplate;
    private final PeriodoService periodoService;

    /**
     * @param productosActivos      productos activos
     * @param valorInventario       valor del inventario a costo promedio
     * @param ventasHoy             total vendido hoy
     * @param facturasHoy           facturas de hoy
     * @param ventasMes             total vendido en el mes
     * @param bajoStockMinimo       productos en o bajo el mínimo
     * @param lotesPorVencer        lotes con existencias que vencen en 30 días o ya vencidos
     * @param reservasActivas       reservas activas
     * @param conflictosSincronizacion operaciones offline en conflicto
     * @param ultimoCierre          último periodo cerrado
     * @param ventasUltimosDias     ventas diarias de los últimos 14 días
     */
    public record ResumenResponse(long productosActivos, BigDecimal valorInventario, BigDecimal ventasHoy,
                                  long facturasHoy, BigDecimal ventasMes, long bajoStockMinimo, long lotesPorVencer,
                                  long reservasActivas, long conflictosSincronizacion, Integer ultimoCierre,
                                  List<Map<String, Object>> ventasUltimosDias) {
    }

    /**
     * @return indicadores
     */
    @GetMapping("/resumen")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORTE_LEER')")
    public ResumenResponse resumen() {
        ZoneId zona = periodoService.zona();
        LocalDate hoy = LocalDate.now(zona);
        OffsetDateTime inicioHoy = hoy.atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime inicio14 = hoy.minusDays(13).atStartOfDay(zona).toOffsetDateTime();
        return new ResumenResponse(
                contar("SELECT count(*) FROM producto WHERE estado = 'ACTIVO'"),
                decimal("SELECT coalesce(sum((s.cantidad_disponible + s.cantidad_reservada + s.cantidad_danada) "
                        + "* p.precio_compra), 0) FROM inventario_saldo s JOIN producto p ON p.id = s.producto_id"),
                decimal("SELECT coalesce(sum(total), 0) FROM factura WHERE fecha >= ?", inicioHoy),
                contar("SELECT count(*) FROM factura WHERE fecha >= ?", inicioHoy),
                decimal("SELECT coalesce(sum(total), 0) FROM factura WHERE fecha >= ?", inicioMes),
                contar("SELECT count(*) FROM inventario_saldo s JOIN producto p ON p.id = s.producto_id "
                        + "WHERE p.estado = 'ACTIVO' AND s.cantidad_disponible <= p.stock_minimo"),
                contar("SELECT count(*) FROM lote WHERE cantidad_actual > 0 AND fecha_vencimiento <= ?",
                        hoy.plusDays(30)),
                contar("SELECT count(*) FROM reserva WHERE estado = 'ACTIVA'"),
                contar("SELECT count(*) FROM sincronizacion_operacion WHERE estado = 'CONFLICTO'"),
                jdbcTemplate.queryForObject("SELECT max(periodo) FROM cierre_mensual", Integer.class),
                jdbcTemplate.queryForList("""
                        SELECT to_char(fecha AT TIME ZONE ?, 'YYYY-MM-DD') AS dia, sum(total) AS total, count(*) AS facturas
                          FROM factura WHERE fecha >= ? GROUP BY 1 ORDER BY 1
                        """, zona.getId(), inicio14));
    }

    private long contar(String sql, Object... args) {
        Long n = jdbcTemplate.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private BigDecimal decimal(String sql, Object... args) {
        BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v;
    }
}
