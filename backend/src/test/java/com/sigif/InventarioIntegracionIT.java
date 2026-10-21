/*
 * Nombre: InventarioIntegracionIT.java
 * Descripción: Pruebas de integración contra PostgreSQL real (Testcontainers): flujo compra/venta,
 *              fracciones, concurrencia sin saldos negativos, triggers de auditoría, periodos
 *              cerrados y contraseñas BCrypt (CP-004..CP-009).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigif.catalogo.CatalogoRepositories.UnidadMedidaRepository;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.compra.CompraDtos.CompraRequest;
import com.sigif.compra.CompraDtos.DocumentoIngresoResponse;
import com.sigif.compra.CompraDtos.LineaCompra;
import com.sigif.compra.CompraService;
import com.sigif.inventario.InventarioRepositories.InventarioSaldoRepository;
import com.sigif.inventario.InventarioSaldo;
import com.sigif.producto.ConversionService;
import com.sigif.producto.ProductoDtos.ProductoRequest;
import com.sigif.producto.ProductoDtos.ProductoResponse;
import com.sigif.producto.ProductoService;
import com.sigif.tercero.Tercero;
import com.sigif.tercero.TerceroDtos.TerceroRequest;
import com.sigif.tercero.TerceroService;
import com.sigif.venta.VentaDtos.FacturaResponse;
import com.sigif.venta.VentaDtos.LineaVenta;
import com.sigif.venta.VentaDtos.VentaRequest;
import com.sigif.venta.VentaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Requiere Docker. Se ejecuta con {@code mvn verify} (Failsafe).
 */
@SpringBootTest(properties = {"sigif.cierre.habilitado=false", "sigif.semilla.usuarios-demo=false"})
@Testcontainers
class InventarioIntegracionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductoService productoService;
    @Autowired
    private CompraService compraService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private TerceroService terceroService;
    @Autowired
    private ConversionService conversionService;
    @Autowired
    private InventarioSaldoRepository saldoRepository;
    @Autowired
    private UnidadMedidaRepository unidadRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Long proveedorId;
    private Long metroId;
    private Long rolloId;
    private Long undId;

    @BeforeEach
    void preparar() {
        autenticar("tester");
        metroId = unidadRepository.findFirstByCodigoIgnoreCase("MT").orElseThrow().getId();
        rolloId = unidadRepository.findFirstByCodigoIgnoreCase("ROLLO").orElseThrow().getId();
        undId = unidadRepository.findFirstByCodigoIgnoreCase("UND").orElseThrow().getId();
        String nit = String.valueOf(900000000 + (System.nanoTime() % 99999999));
        proveedorId = terceroService.crear(new TerceroRequest(Tercero.TipoPersona.JURIDICA, Tercero.TipoDocumento.NIT,
                nit, "1", null, null, "Ferreproveedores SAS", "Calle 1 # 2-3", "6015551234", "ventas@ferre.co",
                false, true)).id();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void flujoCompraVentaFraccionadaYTriggersDeAuditoria() {
        ProductoResponse cable = productoService.crear(producto("CAB-" + System.nanoTime(), metroId, true,
                new BigDecimal("2000")));
        conversionService.agregar(cable.id(), new ConversionService.ConversionRequest(rolloId,
                new BigDecimal("100"), true, true));

        // CA-05.1: compra de 1 rollo (=100 m) a 150.000 genera Documento de Ingreso
        DocumentoIngresoResponse doc = compraService.registrar(new CompraRequest(proveedorId, "FP-1", null,
                List.of(new LineaCompra(cable.id(), rolloId, BigDecimal.ONE, new BigDecimal("150000"), BigDecimal.ZERO,
                        null, null))));
        assertThat(doc.numero()).startsWith("DI-");
        assertThat(saldo(cable.id()).getDisponible()).isEqualByComparingTo("100");
        // Costo promedio: 150.000 / 100 m = 1.500 por metro; precio venta = 1.500 * 1.30
        assertThat(productoService.obtener(cable.id()).precioVentaCalculado()).isEqualByComparingTo("1950");

        // CA-02.1 / CA-06.1: venta de 2.5 m genera Factura y descuenta exacto
        FacturaResponse f = ventaService.registrar(new VentaRequest(null, null,
                List.of(new LineaVenta(cable.id(), null, new BigDecimal("2.5"), null, null, null))));
        assertThat(f.numero()).startsWith("FV-");
        assertThat(f.total()).isEqualByComparingTo("4875.00");
        assertThat(saldo(cable.id()).getDisponible()).isEqualByComparingTo("97.5");

        // CA-10.1: los triggers registran usuario, operación y valores anterior/nuevo
        Integer auditorias = jdbc.queryForObject("""
                SELECT count(*) FROM auditoria WHERE tabla = 'inventario_saldo' AND registro_id = ?
                  AND usuario = 'tester' AND operacion = 'UPDATE' AND datos_anteriores IS NOT NULL
                """, Integer.class, String.valueOf(cable.id()));
        assertThat(auditorias).isGreaterThanOrEqualTo(2);
        Integer movimientos = jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE tabla = 'movimiento_inventario' AND operacion = 'INSERT'",
                Integer.class);
        assertThat(movimientos).isGreaterThanOrEqualTo(2);
    }

    @Test
    void ventaQueExcedeElStockSeRechazaConRollback() {
        ProductoResponse p = productoService.crear(producto("MART-" + System.nanoTime(), undId, false,
                new BigDecimal("10000")));
        comprar(p.id(), 3);
        assertThatThrownBy(() -> ventaService.registrar(new VentaRequest(null, null,
                List.of(new LineaVenta(p.id(), null, new BigDecimal("4"), null, null, null)))))
                .isInstanceOf(ConflictoException.class).hasMessage("inventario.stock.insuficiente");
        assertThat(saldo(p.id()).getDisponible()).isEqualByComparingTo("3");
    }

    @Test
    void ventasConcurrentesNuncaDejanSaldoNegativo() throws Exception {
        ProductoResponse p = productoService.crear(producto("TOR-" + System.nanoTime(), undId, false,
                new BigDecimal("500")));
        comprar(p.id(), 5);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        AtomicInteger exitosas = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tareas.add(pool.submit(() -> {
                autenticar("cajero-concurrente");
                try {
                    ventaService.registrar(new VentaRequest(null, null,
                            List.of(new LineaVenta(p.id(), null, BigDecimal.ONE, null, null, null))));
                    exitosas.incrementAndGet();
                } catch (ConflictoException ex) {
                    // esperado cuando se agota el stock
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }
        for (Future<?> t : tareas) {
            t.get();
        }
        pool.shutdown();
        assertThat(exitosas.get()).isEqualTo(5);
        assertThat(saldo(p.id()).getDisponible()).isEqualByComparingTo("0");
    }

    @Test
    void restriccionCheckImpideSaldosNegativosAunqueFalleLaAplicacion() {
        ProductoResponse p = productoService.crear(producto("CHK-" + System.nanoTime(), undId, false,
                BigDecimal.TEN));
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE inventario_saldo SET cantidad_disponible = -1 WHERE producto_id = ?", p.id()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void triggerBloqueaMovimientosEnPeriodoCerrado() {
        jdbc.update("INSERT INTO cierre_mensual (periodo, tipo_ejecucion, usuario) VALUES (200001, 'MANUAL', 'test') "
                + "ON CONFLICT (periodo) DO NOTHING");
        ProductoResponse p = productoService.crear(producto("PER-" + System.nanoTime(), undId, false, BigDecimal.TEN));
        DocumentoIngresoResponse doc = comprar(p.id(), 1);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO movimiento_inventario (tipo, producto_id, cantidad, estado_destino, documento_ingreso_id,
                                                   usuario, fecha, periodo)
                VALUES ('ENTRADA_COMPRA', ?, 1, 'DISPONIBLE', ?, 'test', '2000-01-15T12:00:00-05:00', 0)
                """, p.id(), doc.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("SIGIF_PERIODO_CERRADO");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM cierre_mensual WHERE periodo = 200001"))
                .hasMessageContaining("SIGIF_CIERRE_INMUTABLE");
    }

    @Test
    void contrasenasSeAlmacenanComoHashBcrypt() {
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE username = 'admin'", String.class);
        assertThat(hash).startsWith("$2a$12$").doesNotContain("Admin123*");
        Integer conPassword = jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE tabla = 'usuario' AND (datos_nuevos -> 'password_hash') IS NOT NULL", Integer.class);
        assertThat(conPassword).isZero();
    }

    private DocumentoIngresoResponse comprar(Long productoId, int cantidad) {
        return compraService.registrar(new CompraRequest(proveedorId, null, null,
                List.of(new LineaCompra(productoId, null, BigDecimal.valueOf(cantidad), new BigDecimal("1000"),
                        BigDecimal.ZERO, null, null))));
    }

    private InventarioSaldo saldo(Long productoId) {
        return saldoRepository.findById(productoId).orElseThrow();
    }

    private ProductoRequest producto(String codigo, Long unidadId, boolean fraccion, BigDecimal costo) {
        return new ProductoRequest("Producto " + codigo, 1L, null, codigo, 1L, 1L, unidadId, fraccion, costo,
                new BigDecimal("30"), BigDecimal.ZERO, false, null, false, null, null);
    }

    private static void autenticar(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }
}
