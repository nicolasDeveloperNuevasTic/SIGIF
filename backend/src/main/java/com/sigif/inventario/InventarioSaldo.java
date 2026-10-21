/*
 * Nombre: InventarioSaldo.java
 * Descripción: Existencias de un producto en la bodega única, separadas por estado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.inventario;

import com.sigif.common.excepcion.ConflictoException;
import com.sigif.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Saldo por estado. Las restricciones CHECK de PostgreSQL garantizan además que ningún
 * saldo quede negativo (RN-008), incluso ante errores de aplicación.
 */
@Entity
@Getter
@Setter
@Table(name = "inventario_saldo")
public class InventarioSaldo {

    @Id
    @Column(name = "producto_id")
    private Long productoId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @Column(name = "cantidad_disponible")
    private BigDecimal disponible = BigDecimal.ZERO;

    @Column(name = "cantidad_reservada")
    private BigDecimal reservada = BigDecimal.ZERO;

    @Column(name = "cantidad_danada")
    private BigDecimal danada = BigDecimal.ZERO;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    /**
     * @return existencias físicas totales
     */
    public BigDecimal total() {
        return disponible.add(reservada).add(danada);
    }

    /**
     * @param estado estado
     * @return cantidad en ese estado
     */
    public BigDecimal cantidad(EstadoInventario estado) {
        return switch (estado) {
            case DISPONIBLE -> disponible;
            case RESERVADA -> reservada;
            case DANADA -> danada;
        };
    }

    /**
     * Suma (o resta, si es negativa) una cantidad al estado indicado. Rechaza cualquier
     * resultado negativo (REQ-F-015, REQ-F-017).
     *
     * @param estado   estado afectado
     * @param cantidad delta
     * @param nombreProducto para el mensaje de error
     */
    public void ajustar(EstadoInventario estado, BigDecimal cantidad, String nombreProducto) {
        BigDecimal nuevo = cantidad(estado).add(cantidad);
        if (nuevo.signum() < 0) {
            throw new ConflictoException("inventario.stock.insuficiente", nombreProducto,
                    cantidad(estado).stripTrailingZeros().toPlainString(), estado.name());
        }
        switch (estado) {
            case DISPONIBLE -> disponible = nuevo;
            case RESERVADA -> reservada = nuevo;
            case DANADA -> danada = nuevo;
        }
    }
}
