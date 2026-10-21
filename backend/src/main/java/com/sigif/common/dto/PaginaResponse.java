/*
 * Nombre: PaginaResponse.java
 * Descripción: Envoltorio JSON estable para respuestas paginadas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Página de resultados.
 *
 * @param contenido      elementos de la página
 * @param pagina         número de página (base 0)
 * @param tamano         tamaño de página
 * @param totalElementos total de elementos
 * @param totalPaginas   total de páginas
 * @param <T>            tipo de elemento
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    /**
     * Convierte una página de entidades en una página de DTOs.
     *
     * @param page   página de origen
     * @param mapper función de conversión
     * @param <E>    tipo de entidad
     * @param <T>    tipo de DTO
     * @return página convertida
     */
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
