/*
 * Nombre: SincronizacionProcesador.java
 * Descripción: Aplica cada venta offline en su propia transacción y registra su resultado.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.sincronizacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigif.common.util.UsuarioActual;
import com.sigif.sincronizacion.SincronizacionDtos.ResultadoSincronizacion;
import com.sigif.sincronizacion.SincronizacionDtos.VentaOffline;
import com.sigif.venta.Factura;
import com.sigif.venta.VentaService;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Una venta en conflicto no afecta a las demás del lote: cada una usa REQUIRES_NEW. La regla de
 * cero saldos negativos se valida contra el stock real del servidor al sincronizar (REQ-F-039).
 */
@Service
@RequiredArgsConstructor
public class SincronizacionProcesador {

    private final SincronizacionRepository repository;
    private final VentaService ventaService;
    private final ObjectMapper objectMapper;

    /**
     * @param idLocal UUID
     * @return resultado previo si la operación ya fue recibida
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ResultadoSincronizacion> previo(java.util.UUID idLocal) {
        return repository.findByIdLocal(idLocal).map(o -> new ResultadoSincronizacion(o.getIdLocal(),
                o.getEstado() == SincronizacionOperacion.Estado.CONFLICTO ? "CONFLICTO" : "DUPLICADA",
                o.getFactura() == null ? null : o.getFactura().getId(),
                o.getFactura() == null ? null : o.getFactura().getNumero(), o.getMensaje()));
    }

    /**
     * @param v venta offline
     * @return resultado APLICADA
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResultadoSincronizacion aplicar(VentaOffline v) {
        Factura f = ventaService.registrar(conObservacion(v), Factura.Origen.OFFLINE, v.idLocal());
        SincronizacionOperacion o = nueva(v);
        o.setEstado(SincronizacionOperacion.Estado.APLICADA);
        o.setFactura(f);
        repository.save(o);
        return new ResultadoSincronizacion(v.idLocal(), "APLICADA", f.getId(), f.getNumero(), null);
    }

    /**
     * @param v       venta offline
     * @param mensaje causa del conflicto
     * @return resultado CONFLICTO
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResultadoSincronizacion conflicto(VentaOffline v, String mensaje) {
        SincronizacionOperacion o = nueva(v);
        o.setEstado(SincronizacionOperacion.Estado.CONFLICTO);
        o.setMensaje(mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje);
        repository.save(o);
        return new ResultadoSincronizacion(v.idLocal(), "CONFLICTO", null, null, o.getMensaje());
    }

    /**
     * Reintenta una operación en conflicto (ej. después de registrar una compra).
     *
     * @param operacionId operación
     * @return operación resuelta
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SincronizacionOperacion reintentar(Long operacionId) {
        SincronizacionOperacion o = repository.findById(operacionId).orElseThrow();
        VentaOffline v = leer(o.getPayload());
        Factura f = ventaService.registrar(conObservacion(v), Factura.Origen.OFFLINE, v.idLocal());
        o.setFactura(f);
        o.setEstado(SincronizacionOperacion.Estado.RESUELTA);
        o.setResueltoPor(UsuarioActual.nombre());
        o.setFechaResolucion(OffsetDateTime.now());
        return o;
    }

    private VentaOffline leer(String json) {
        try {
            return objectMapper.readValue(json, VentaOffline.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private com.sigif.venta.VentaDtos.VentaRequest conObservacion(VentaOffline v) {
        String obs = "Venta offline " + (v.dispositivo() == null ? "" : v.dispositivo() + " ") + v.fechaLocal()
                + (v.venta().observaciones() == null ? "" : " - " + v.venta().observaciones());
        return new com.sigif.venta.VentaDtos.VentaRequest(v.venta().clienteId(),
                obs.length() > 500 ? obs.substring(0, 500) : obs, v.venta().lineas());
    }

    private SincronizacionOperacion nueva(VentaOffline v) {
        SincronizacionOperacion o = new SincronizacionOperacion();
        o.setIdLocal(v.idLocal());
        o.setDispositivo(v.dispositivo());
        o.setFechaLocal(v.fechaLocal());
        o.setUsuario(UsuarioActual.nombre());
        try {
            o.setPayload(objectMapper.writeValueAsString(v));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
        return o;
    }
}
