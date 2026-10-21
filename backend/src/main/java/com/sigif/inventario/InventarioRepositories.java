/*
 * Nombre: InventarioRepositories.java
 * Descripción: Repositorios JPA de saldos, lotes, movimientos y reservas, con bloqueos pesimistas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorios del módulo de inventario. Los métodos {@code bloquear*} usan
 * {@code SELECT ... FOR UPDATE} para serializar ventas concurrentes (REQ-F-024).
 */
public final class InventarioRepositories {

    private InventarioRepositories() {
    }

    /** Repositorio de saldos. */
    public interface InventarioSaldoRepository extends JpaRepository<InventarioSaldo, Long>,
            JpaSpecificationExecutor<InventarioSaldo> {

        /**
         * @param productoId producto
         * @return saldo bloqueado para escritura
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select s from InventarioSaldo s where s.productoId = :productoId")
        Optional<InventarioSaldo> bloquear(@Param("productoId") Long productoId);

        /**
         * @param productoIds productos
         * @return saldos
         */
        List<InventarioSaldo> findByProductoIdIn(List<Long> productoIds);

        /**
         * @return saldos con disponible menor o igual al stock mínimo de productos activos
         */
        @Query("select s from InventarioSaldo s join fetch s.producto p "
                + "where p.estado = com.sigif.producto.EstadoProducto.ACTIVO and s.disponible <= p.stockMinimo "
                + "order by p.nombre")
        List<InventarioSaldo> bajoStockMinimo();

        /**
         * @return valor del inventario a costo promedio
         */
        @Query("select coalesce(sum((s.disponible + s.reservada + s.danada) * s.producto.precioCompra), 0) "
                + "from InventarioSaldo s")
        BigDecimal valorInventario();
    }

    /** Repositorio de lotes. */
    public interface LoteRepository extends JpaRepository<Lote, Long> {

        /**
         * @param productoId producto
         * @param hoy        fecha actual
         * @return lotes vigentes con existencias, bloqueados, ordenados FEFO
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select l from Lote l where l.producto.id = :productoId and l.cantidadActual > 0 "
                + "and l.fechaVencimiento >= :hoy order by l.fechaVencimiento asc, l.id asc")
        List<Lote> bloquearVigentes(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

        /**
         * @param productoId producto
         * @param codigoLote código
         * @return lote
         */
        Optional<Lote> findByProductoIdAndCodigoLote(Long productoId, String codigoLote);

        /**
         * @param productoId producto
         * @return lotes del producto
         */
        List<Lote> findByProductoIdOrderByFechaVencimientoAsc(Long productoId);

        /**
         * @param limite fecha límite
         * @return lotes con existencias que vencen hasta la fecha límite (incluye vencidos)
         */
        @EntityGraph(attributePaths = {"producto"})
        @Query("select l from Lote l where l.cantidadActual > 0 and l.fechaVencimiento <= :limite "
                + "order by l.fechaVencimiento asc")
        List<Lote> porVencer(@Param("limite") LocalDate limite);

        /**
         * @param productoId producto
         * @return fecha de vencimiento más próxima con existencias
         */
        @Query("select min(l.fechaVencimiento) from Lote l where l.producto.id = :productoId and l.cantidadActual > 0")
        Optional<LocalDate> proximoVencimiento(@Param("productoId") Long productoId);
    }

    /** Repositorio de movimientos. */
    public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long>,
            JpaSpecificationExecutor<MovimientoInventario> {
    }

    /** Repositorio de reservas. */
    public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

        /**
         * @param id id
         * @return reserva bloqueada
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select r from Reserva r where r.id = :id")
        Optional<Reserva> bloquear(@Param("id") Long id);

        /**
         * @param estado estado
         * @param fecha  fecha de corte
         * @return ids de reservas vencidas
         */
        @Query("select r.id from Reserva r where r.estado = :estado and r.fechaExpiracion < :fecha")
        List<Long> idsVencidas(@Param("estado") Reserva.Estado estado, @Param("fecha") OffsetDateTime fecha);
    }
}
