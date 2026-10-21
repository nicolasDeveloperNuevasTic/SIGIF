/*
 * Nombre: ManejadorErroresSeguridad.java
 * Descripción: Respuestas JSON i18n para 401/403 generados por la cadena de filtros, con auditoría.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigif.auditoria.AuditoriaService;
import com.sigif.common.dto.ApiError;
import com.sigif.common.util.Mensajes;
import com.sigif.common.util.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Punto de entrada (401) y manejador de acceso denegado (403). Registra cada intento en la
 * bitácora de auditoría (REQ-F-032).
 */
@Component
@RequiredArgsConstructor
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Mensajes mensajes;
    private final AuditoriaService auditoriaService;
    private final LocaleResolver localeResolver;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        auditoriaService.registrarEventoSeguridad("ACCESO_DENEGADO_401", "ANONIMO",
                request.getMethod() + " " + request.getRequestURI());
        escribir(request, response, HttpStatus.UNAUTHORIZED, "error.no.autenticado");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        auditoriaService.registrarEventoSeguridad("ACCESO_DENEGADO_403", UsuarioActual.nombre(),
                request.getMethod() + " " + request.getRequestURI());
        escribir(request, response, HttpStatus.FORBIDDEN, "error.acceso.denegado");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus estado, String clave)
            throws IOException {
        LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
        ApiError error = new ApiError(OffsetDateTime.now(), estado.value(), estado.getReasonPhrase(), clave,
                mensajes.obtener(clave), request.getRequestURI(), List.of());
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
