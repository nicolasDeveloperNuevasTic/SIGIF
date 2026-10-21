/*
 * Nombre: TerceroService.java
 * Descripción: Reglas y CRUD de terceros (clientes y proveedores).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.tercero;

import com.sigif.common.dto.PaginaResponse;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.tercero.Tercero.TipoDocumento;
import com.sigif.tercero.Tercero.TipoPersona;
import com.sigif.tercero.TerceroDtos.TerceroRequest;
import com.sigif.tercero.TerceroDtos.TerceroResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persona natural: cédula (CC/CE/PAS), nombre y apellido obligatorios. Persona jurídica: NIT y
 * razón social obligatorios. Dirección, teléfono y correo obligatorios en ambos (REQ-F-025/026).
 */
@Service
@RequiredArgsConstructor
public class TerceroService {

    private final TerceroRepository repository;

    /** Filtro de rol del tercero. */
    public enum Rol {
        /** Solo clientes. */
        CLIENTE,
        /** Solo proveedores. */
        PROVEEDOR
    }

    /**
     * @param rol    filtro por rol
     * @param texto  busca en documento, nombre, apellido y razón social
     * @param pagina página
     * @param tamano tamaño
     * @return página de terceros
     */
    @Transactional(readOnly = true)
    public PaginaResponse<TerceroResponse> buscar(Rol rol, String texto, int pagina, int tamano) {
        Specification<Tercero> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (rol == Rol.CLIENTE) {
                p.add(cb.isTrue(root.<Boolean>get("esCliente")));
            } else if (rol == Rol.PROVEEDOR) {
                p.add(cb.isTrue(root.<Boolean>get("esProveedor")));
            }
            if (texto != null && !texto.isBlank()) {
                String like = "%" + texto.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.<String>get("numeroDocumento")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("nombre"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("apellido"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("razonSocial"), "")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(repository.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 200),
                Sort.by("esGenerico").descending().and(Sort.by("id")))), TerceroResponse::de);
    }

    /**
     * @param id id
     * @return tercero
     */
    @Transactional(readOnly = true)
    public TerceroResponse obtener(Long id) {
        return TerceroResponse.de(entidad(id));
    }

    /**
     * @param req datos
     * @return tercero creado
     */
    @Transactional
    public TerceroResponse crear(TerceroRequest req) {
        if (repository.existsByNumeroDocumento(req.numeroDocumento().trim())) {
            throw new ConflictoException("tercero.documento.duplicado", req.numeroDocumento());
        }
        Tercero t = new Tercero();
        aplicar(t, req);
        return TerceroResponse.de(repository.save(t));
    }

    /**
     * @param id  id
     * @param req datos
     * @return tercero actualizado
     */
    @Transactional
    public TerceroResponse actualizar(Long id, TerceroRequest req) {
        Tercero t = entidad(id);
        if (t.isEsGenerico()) {
            throw new ReglaNegocioException("tercero.generico.inmutable");
        }
        if (repository.existsByNumeroDocumentoAndIdNot(req.numeroDocumento().trim(), id)) {
            throw new ConflictoException("tercero.documento.duplicado", req.numeroDocumento());
        }
        aplicar(t, req);
        return TerceroResponse.de(t);
    }

    /**
     * @param id     id
     * @param activo estado
     * @return tercero
     */
    @Transactional
    public TerceroResponse cambiarEstado(Long id, boolean activo) {
        Tercero t = entidad(id);
        if (t.isEsGenerico()) {
            throw new ReglaNegocioException("tercero.generico.inmutable");
        }
        t.setActivo(activo);
        return TerceroResponse.de(t);
    }

    /**
     * @param id id
     * @return proveedor activo
     */
    @Transactional(readOnly = true)
    public Tercero proveedorValido(Long id) {
        Tercero t = repository.findById(id == null ? -1L : id)
                .orElseThrow(() -> new ReglaNegocioException("compra.proveedor.invalido"));
        if (!t.isActivo() || !t.isEsProveedor()) {
            throw new ReglaNegocioException("compra.proveedor.invalido");
        }
        return t;
    }

    /**
     * @param id id (null = Consumidor Final)
     * @return cliente activo
     */
    @Transactional(readOnly = true)
    public Tercero clienteValido(Long id) {
        Tercero t = (id == null ? repository.findByNumeroDocumento(CONSUMIDOR_FINAL) : repository.findById(id))
                .orElseThrow(() -> new ReglaNegocioException("venta.cliente.invalido"));
        if (!t.isActivo() || !t.isEsCliente()) {
            throw new ReglaNegocioException("venta.cliente.invalido");
        }
        return t;
    }

    /** Documento del cliente genérico Consumidor Final. */
    public static final String CONSUMIDOR_FINAL = "222222222222";

    /** Documento del proveedor genérico para el saldo inicial migrado. */
    public static final String SALDO_INICIAL = "000000000";

    /**
     * Valida reglas por tipo de persona y copia los datos.
     *
     * @param t   entidad destino
     * @param req datos
     */
    static void aplicar(Tercero t, TerceroRequest req) {
        if (!req.esCliente() && !req.esProveedor()) {
            throw new ReglaNegocioException("tercero.rol.obligatorio");
        }
        if (req.tipoPersona() == TipoPersona.NATURAL) {
            if (vacio(req.nombre()) || vacio(req.apellido())) {
                throw new ReglaNegocioException("tercero.natural.nombre");
            }
        } else {
            if (req.tipoDocumento() != TipoDocumento.NIT) {
                throw new ReglaNegocioException("tercero.juridica.nit");
            }
            if (vacio(req.razonSocial())) {
                throw new ReglaNegocioException("tercero.juridica.razon");
            }
        }
        if ((req.tipoDocumento() == TipoDocumento.CC || req.tipoDocumento() == TipoDocumento.NIT)
                && !req.numeroDocumento().trim().matches("\\d+")) {
            throw new ReglaNegocioException("validacion.documento");
        }
        t.setTipoPersona(req.tipoPersona());
        t.setTipoDocumento(req.tipoDocumento());
        t.setNumeroDocumento(req.numeroDocumento().trim());
        t.setDigitoVerificacion(vacio(req.digitoVerificacion()) ? null : req.digitoVerificacion());
        t.setNombre(limpiar(req.nombre()));
        t.setApellido(limpiar(req.apellido()));
        t.setRazonSocial(limpiar(req.razonSocial()));
        t.setDireccion(req.direccion().trim());
        t.setTelefono(req.telefono().trim());
        t.setCorreo(req.correo().trim().toLowerCase());
        t.setEsCliente(req.esCliente());
        t.setEsProveedor(req.esProveedor());
    }

    private Tercero entidad(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("recurso.tercero", id));
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpiar(String s) {
        return vacio(s) ? null : s.trim();
    }
}
