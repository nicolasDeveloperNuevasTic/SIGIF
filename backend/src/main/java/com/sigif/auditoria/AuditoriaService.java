/*
 * Nombre: AuditoriaService.java
 * Descripción: Consulta filtrada de auditoría y registro de eventos de seguridad.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.auditoria;

import com.sigif.common.dto.PaginaResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de auditoría (REQ-F-033 a REQ-F-035). Los cambios de entidades los registran
 * los triggers de PostgreSQL; aquí se registran los eventos de seguridad (401/403) y se
 * expone la consulta filtrada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Registra un evento de seguridad sin depender de la transacción en curso.
     *
     * @param operacion tipo de evento (ACCESO_DENEGADO_401, ACCESO_DENEGADO_403, LOGIN_FALLIDO, LOGIN)
     * @param usuario   usuario involucrado
     * @param detalle   ruta o descripción
     */
    public void registrarEventoSeguridad(String operacion, String usuario, String detalle) {
        try {
            jdbcTemplate.update("INSERT INTO auditoria (tabla, operacion, usuario, detalle) VALUES ('SEGURIDAD', ?, ?, ?)",
                    operacion, usuario, detalle == null ? null : detalle.substring(0, Math.min(500, detalle.length())));
        } catch (RuntimeException ex) {
            log.warn("No fue posible registrar el evento de seguridad {}: {}", operacion, ex.getMessage());
        }
    }

    /**
     * Consulta paginada con filtros opcionales.
     *
     * @param tabla     tabla auditada
     * @param operacion operación (INSERT, UPDATE, DELETE, ACCESO_DENEGADO_403...)
     * @param usuario   usuario
     * @param registroId id del registro
     * @param desde     fecha inicial
     * @param hasta     fecha final
     * @param pagina    página
     * @param tamano    tamaño
     * @return página de registros
     */
    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> buscar(String tabla, String operacion, String usuario, String registroId,
                                                    OffsetDateTime desde, OffsetDateTime hasta, int pagina, int tamano) {
        Specification<Auditoria> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (tabla != null && !tabla.isBlank()) {
                p.add(cb.equal(root.get("tabla"), tabla));
            }
            if (operacion != null && !operacion.isBlank()) {
                p.add(cb.equal(root.get("operacion"), operacion));
            }
            if (usuario != null && !usuario.isBlank()) {
                p.add(cb.equal(root.get("usuario"), usuario));
            }
            if (registroId != null && !registroId.isBlank()) {
                p.add(cb.equal(root.get("registroId"), registroId));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("fecha"), desde));
            }
            if (hasta != null) {
                p.add(cb.lessThanOrEqualTo(root.<OffsetDateTime>get("fecha"), hasta));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(repository.findAll(spec,
                PageRequest.of(pagina, Math.min(tamano, 200), Sort.by(Sort.Direction.DESC, "id"))),
                AuditoriaResponse::de);
    }

    /**
     * Registro de auditoría expuesto por el API.
     *
     * @param id              identificador
     * @param tabla           tabla
     * @param operacion       operación
     * @param registroId      id del registro afectado
     * @param usuario         usuario
     * @param fecha           fecha/hora
     * @param datosAnteriores JSON anterior
     * @param datosNuevos     JSON nuevo
     * @param detalle         detalle adicional
     */
    public record AuditoriaResponse(Long id, String tabla, String operacion, String registroId, String usuario,
                                    OffsetDateTime fecha, String datosAnteriores, String datosNuevos, String detalle) {
        static AuditoriaResponse de(Auditoria a) {
            return new AuditoriaResponse(a.getId(), a.getTabla(), a.getOperacion(), a.getRegistroId(), a.getUsuario(),
                    a.getFecha(), a.getDatosAnteriores(), a.getDatosNuevos(), a.getDetalle());
        }
    }
}
