/*
 * Nombre: GlobalExceptionHandler.java
 * Descripción: Traducción centralizada de excepciones a respuestas JSON internacionalizadas.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.common.excepcion;

import com.sigif.auditoria.AuditoriaService;
import com.sigif.common.dto.ApiError;
import com.sigif.common.util.Mensajes;
import com.sigif.common.util.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejador global: convierte excepciones de negocio, validación, seguridad y base de datos
 * en {@link ApiError} con el mensaje traducido según la cabecera Accept-Language (REQ-F-005).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Mensajes mensajes;
    private final AuditoriaService auditoriaService;

    /**
     * @param ex      excepción de negocio
     * @param request petición
     * @return error con el estado de la excepción
     */
    @ExceptionHandler(SigifException.class)
    public ResponseEntity<ApiError> negocio(SigifException ex, HttpServletRequest request) {
        return construir(ex.getEstado(), ex.getClave(), mensajes.obtener(ex.getClave(), ex.getArgumentos()),
                request, List.of());
    }

    /**
     * @param ex      errores de Bean Validation sobre el cuerpo
     * @param request petición
     * @return 400 con errores por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.ErrorCampo> errores = ex.getBindingResult().getAllErrors().stream()
                .map(e -> new ApiError.ErrorCampo(
                        e instanceof FieldError fe ? fe.getField() : e.getObjectName(),
                        e.getDefaultMessage()))
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "error.validacion", mensajes.obtener("error.validacion"),
                request, errores);
    }

    /**
     * @param ex      violaciones de restricciones en parámetros
     * @param request petición
     * @return 400 con errores
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restricciones(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.ErrorCampo> errores = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.ErrorCampo(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "error.validacion", mensajes.obtener("error.validacion"),
                request, errores);
    }

    /**
     * @param ex      cuerpo ilegible o parámetro mal tipado
     * @param request petición
     * @return 400
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiError> peticionInvalida(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "error.peticion.invalida",
                mensajes.obtener("error.peticion.invalida"), request, List.of());
    }

    /**
     * @param ex      credenciales inválidas o usuario inactivo
     * @param request petición
     * @return 401
     */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> credenciales(Exception ex, HttpServletRequest request) {
        auditoriaService.registrarEventoSeguridad("LOGIN_FALLIDO", UsuarioActual.nombre(), request.getRequestURI());
        return construir(HttpStatus.UNAUTHORIZED, "error.credenciales", mensajes.obtener("error.credenciales"),
                request, List.of());
    }

    /**
     * @param ex      acceso denegado por permisos (@PreAuthorize)
     * @param request petición
     * @return 403 registrado en auditoría (REQ-F-032)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        auditoriaService.registrarEventoSeguridad("ACCESO_DENEGADO_403", UsuarioActual.nombre(),
                request.getMethod() + " " + request.getRequestURI());
        return construir(HttpStatus.FORBIDDEN, "error.acceso.denegado", mensajes.obtener("error.acceso.denegado"),
                request, List.of());
    }

    /**
     * @param ex      conflicto de concurrencia
     * @param request petición
     * @return 409
     */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiError> concurrencia(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, "error.concurrencia", mensajes.obtener("error.concurrencia"),
                request, List.of());
    }

    /**
     * Traduce errores lanzados por restricciones y triggers de PostgreSQL.
     *
     * @param ex      excepción de acceso a datos
     * @param request petición
     * @return 409 con el mensaje de la regla violada
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> baseDatos(DataAccessException ex, HttpServletRequest request) {
        String detalle = String.valueOf(ex.getMostSpecificCause().getMessage());
        String clave;
        if (detalle.contains("SIGIF_PERIODO_CERRADO")) {
            clave = "cierre.periodo.cerrado.generico";
        } else if (detalle.contains("ck_saldo_")) {
            clave = "inventario.saldo.negativo";
        } else if (detalle.contains("ux_producto_codigo_barras")) {
            clave = "producto.codigo.duplicado.generico";
        } else if (detalle.contains("ux_tercero_documento")) {
            clave = "tercero.documento.duplicado.generico";
        } else if (ex instanceof DataIntegrityViolationException) {
            clave = "error.integridad";
        } else {
            log.error("Error de base de datos", ex);
            return construir(HttpStatus.INTERNAL_SERVER_ERROR, "error.interno", mensajes.obtener("error.interno"),
                    request, List.of());
        }
        return construir(HttpStatus.CONFLICT, clave, mensajes.obtener(clave), request, List.of());
    }

    /**
     * @param ex      ruta inexistente
     * @param request petición
     * @return 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaNoEncontrada(NoResourceFoundException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "error.ruta.no.encontrada",
                mensajes.obtener("error.ruta.no.encontrada"), request, List.of());
    }

    /**
     * @param ex      error no controlado
     * @param request petición
     * @return 500 sin detalles internos
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> general(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "error.interno", mensajes.obtener("error.interno"),
                request, List.of());
    }

    private ResponseEntity<ApiError> construir(HttpStatus estado, String codigo, String mensaje,
                                               HttpServletRequest request, List<ApiError.ErrorCampo> errores) {
        ApiError error = new ApiError(OffsetDateTime.now(), estado.value(), estado.getReasonPhrase(), codigo,
                mensaje, request.getRequestURI(), errores);
        return ResponseEntity.status(estado).body(error);
    }
}
