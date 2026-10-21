/*
 * Nombre: CatalogoService.java
 * Descripción: CRUD de catálogos maestros: categorías, marcas, presentaciones y unidades de medida.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import com.sigif.catalogo.CatalogoRepositories.CategoriaRepository;
import com.sigif.catalogo.CatalogoRepositories.MarcaRepository;
import com.sigif.catalogo.CatalogoRepositories.PresentacionRepository;
import com.sigif.catalogo.CatalogoRepositories.UnidadMedidaRepository;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administra los catálogos de soporte del producto. El tipo se indica con {@link TipoCatalogo}.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final PresentacionRepository presentacionRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    /** Tipos de catálogo expuestos en /api/v1/catalogos/{tipo}. */
    public enum TipoCatalogo {
        /** Categorías. */
        CATEGORIAS,
        /** Marcas. */
        MARCAS,
        /** Presentaciones. */
        PRESENTACIONES,
        /** Unidades de medida. */
        UNIDADES;

        /**
         * @param ruta segmento de ruta (categorias, marcas, presentaciones, unidades-medida)
         * @return tipo
         */
        public static TipoCatalogo deRuta(String ruta) {
            return switch (ruta) {
                case "categorias" -> CATEGORIAS;
                case "marcas" -> MARCAS;
                case "presentaciones" -> PRESENTACIONES;
                case "unidades-medida" -> UNIDADES;
                default -> throw new RecursoNoEncontradoException("recurso.catalogo", ruta);
            };
        }
    }

    /**
     * @param nombre          nombre
     * @param descripcion     descripción
     * @param codigo          código (obligatorio para unidades)
     * @param categoriaPadreId categoría padre (solo categorías)
     * @param activo          activo
     */
    public record CatalogoRequest(
            @NotBlank(message = "{validacion.obligatorio}") @Size(max = 120, message = "{validacion.longitud}") String nombre,
            @Size(max = 255, message = "{validacion.longitud}") String descripcion,
            @Size(max = 10, message = "{validacion.longitud}") String codigo,
            Long categoriaPadreId,
            Boolean activo) {
    }

    /**
     * @param id               id
     * @param nombre           nombre
     * @param descripcion      descripción
     * @param codigo           código (unidades)
     * @param categoriaPadreId padre (categorías)
     * @param activo           activo
     */
    public record CatalogoResponse(Long id, String nombre, String descripcion, String codigo, Long categoriaPadreId,
                                   boolean activo) {

        static CatalogoResponse de(CatalogoBase c) {
            String codigo = c instanceof UnidadMedida u ? u.getCodigo() : null;
            Long padre = c instanceof Categoria cat && cat.getPadre() != null ? cat.getPadre().getId() : null;
            return new CatalogoResponse(c.getId(), c.getNombre(), c.getDescripcion(), codigo, padre, c.isActivo());
        }
    }

    /**
     * @param tipo tipo de catálogo
     * @return elementos ordenados por nombre
     */
    @Transactional(readOnly = true)
    public List<CatalogoResponse> listar(TipoCatalogo tipo) {
        return repositorio(tipo).findAll(Sort.by("nombre")).stream().map(CatalogoResponse::de).toList();
    }

    /**
     * @param tipo tipo
     * @param req  datos
     * @return elemento creado
     */
    @Transactional
    public CatalogoResponse crear(TipoCatalogo tipo, CatalogoRequest req) {
        CatalogoBase entidad = switch (tipo) {
            case CATEGORIAS -> new Categoria();
            case MARCAS -> new Marca();
            case PRESENTACIONES -> new Presentacion();
            case UNIDADES -> new UnidadMedida();
        };
        aplicar(entidad, req);
        return CatalogoResponse.de(guardar(tipo, entidad, req.nombre()));
    }

    /**
     * @param tipo tipo
     * @param id   id
     * @param req  datos
     * @return elemento actualizado
     */
    @Transactional
    public CatalogoResponse actualizar(TipoCatalogo tipo, Long id, CatalogoRequest req) {
        CatalogoBase entidad = repositorio(tipo).findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.catalogo", id));
        aplicar(entidad, req);
        return CatalogoResponse.de(guardar(tipo, entidad, req.nombre()));
    }

    /**
     * Obtiene una categoría por nombre o la crea (usado por la migración Excel).
     *
     * @param nombre nombre
     * @return categoría
     */
    @Transactional
    public Categoria categoria(String nombre) {
        return obtenerOCrear(categoriaRepository.findFirstByNombreIgnoreCase(nombre.trim()), () -> {
            Categoria c = new Categoria();
            c.setNombre(nombre.trim());
            return categoriaRepository.save(c);
        });
    }

    /**
     * @param nombre nombre
     * @return marca existente o creada
     */
    @Transactional
    public Marca marca(String nombre) {
        return obtenerOCrear(marcaRepository.findFirstByNombreIgnoreCase(nombre.trim()), () -> {
            Marca m = new Marca();
            m.setNombre(nombre.trim());
            return marcaRepository.save(m);
        });
    }

    /**
     * @param nombre nombre
     * @return presentación existente o creada
     */
    @Transactional
    public Presentacion presentacion(String nombre) {
        return obtenerOCrear(presentacionRepository.findFirstByNombreIgnoreCase(nombre.trim()), () -> {
            Presentacion p = new Presentacion();
            p.setNombre(nombre.trim());
            return presentacionRepository.save(p);
        });
    }

    /**
     * @param codigo código de unidad
     * @return unidad existente
     */
    @Transactional(readOnly = true)
    public UnidadMedida unidadPorCodigo(String codigo) {
        return unidadMedidaRepository.findFirstByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.unidad", codigo));
    }

    private <T> T obtenerOCrear(java.util.Optional<T> existente, Supplier<T> creador) {
        return existente.orElseGet(creador);
    }

    private void aplicar(CatalogoBase entidad, CatalogoRequest req) {
        entidad.setNombre(req.nombre().trim());
        entidad.setDescripcion(req.descripcion());
        if (req.activo() != null) {
            entidad.setActivo(req.activo());
        }
        if (entidad instanceof UnidadMedida u) {
            if (req.codigo() == null || req.codigo().isBlank()) {
                throw new ReglaNegocioException("catalogo.unidad.codigo");
            }
            u.setCodigo(req.codigo().trim().toUpperCase());
        }
        if (entidad instanceof Categoria c) {
            if (req.categoriaPadreId() == null) {
                c.setPadre(null);
            } else {
                if (req.categoriaPadreId().equals(c.getId())) {
                    throw new ReglaNegocioException("catalogo.categoria.ciclo");
                }
                c.setPadre(categoriaRepository.findById(req.categoriaPadreId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("recurso.categoria", req.categoriaPadreId())));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private CatalogoBase guardar(TipoCatalogo tipo, CatalogoBase entidad, String nombre) {
        try {
            return ((JpaRepository<CatalogoBase, Long>) repositorio(tipo)).saveAndFlush(entidad);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictoException("catalogo.duplicado", nombre);
        }
    }

    private JpaRepository<? extends CatalogoBase, Long> repositorio(TipoCatalogo tipo) {
        return switch (tipo) {
            case CATEGORIAS -> categoriaRepository;
            case MARCAS -> marcaRepository;
            case PRESENTACIONES -> presentacionRepository;
            case UNIDADES -> unidadMedidaRepository;
        };
    }
}
