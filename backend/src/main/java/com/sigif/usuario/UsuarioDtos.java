/*
 * Nombre: UsuarioDtos.java
 * Descripción: DTOs JSON del módulo de seguridad (autenticación, usuarios, roles y permisos).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Agrupa los registros (records) de entrada y salida del módulo de usuarios.
 */
public final class UsuarioDtos {

    /** Política de contraseñas: 8+ caracteres, mayúscula, minúscula y dígito. */
    public static final String PATRON_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";

    private UsuarioDtos() {
    }

    /**
     * @param username usuario
     * @param password contraseña
     */
    public record LoginRequest(@NotBlank(message = "{validacion.obligatorio}") String username,
                               @NotBlank(message = "{validacion.obligatorio}") String password) {
    }

    /**
     * @param token          JWT
     * @param tipo           "Bearer"
     * @param expiraEn       segundos de vigencia
     * @param usuario        datos del usuario
     */
    public record LoginResponse(String token, String tipo, long expiraEn, UsuarioResponse usuario) {
    }

    /**
     * @param username       usuario
     * @param password       contraseña (solo creación)
     * @param nombreCompleto nombre
     * @param correo         correo
     * @param idioma         es | en
     * @param roles          códigos de rol
     */
    public record UsuarioRequest(
            @NotBlank(message = "{validacion.obligatorio}") @Size(min = 3, max = 60, message = "{validacion.longitud}")
            String username,
            @Pattern(regexp = PATRON_PASSWORD, message = "{validacion.password}") String password,
            @NotBlank(message = "{validacion.obligatorio}") @Size(max = 150, message = "{validacion.longitud}")
            String nombreCompleto,
            @NotBlank(message = "{validacion.obligatorio}") @Email(message = "{validacion.correo}") String correo,
            @Pattern(regexp = "es|en", message = "{validacion.idioma}") String idioma,
            @NotEmpty(message = "{validacion.obligatorio}") List<String> roles) {
    }

    /**
     * @param password nueva contraseña
     */
    public record CambioPasswordRequest(
            @NotBlank(message = "{validacion.obligatorio}")
            @Pattern(regexp = PATRON_PASSWORD, message = "{validacion.password}") String password) {
    }

    /**
     * @param activo nuevo estado
     */
    public record EstadoUsuarioRequest(boolean activo) {
    }

    /**
     * @param id             id
     * @param username       usuario
     * @param nombreCompleto nombre
     * @param correo         correo
     * @param idioma         idioma preferido
     * @param activo         activo
     * @param tieneApiKey    tiene API Key de integración
     * @param ultimoAcceso   último acceso
     * @param roles          roles
     * @param autoridades    roles y permisos efectivos
     */
    public record UsuarioResponse(Long id, String username, String nombreCompleto, String correo, String idioma,
                                  boolean activo, boolean tieneApiKey, OffsetDateTime ultimoAcceso,
                                  List<String> roles, Set<String> autoridades) {

        /**
         * @param u entidad
         * @return DTO
         */
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getCorreo(), u.getIdioma(),
                    u.isActivo(), u.getApiKeyHash() != null, u.getUltimoAcceso(),
                    u.getRoles().stream().map(Rol::getCodigo).sorted().toList(), u.autoridades());
        }
    }

    /**
     * @param apiKey API Key en texto plano (se muestra una sola vez)
     */
    public record ApiKeyResponse(String apiKey) {
    }

    /**
     * @param id          id
     * @param codigo      código
     * @param modulo      módulo
     * @param descripcion descripción
     */
    public record PermisoResponse(Long id, String codigo, String modulo, String descripcion) {

        /**
         * @param p entidad
         * @return DTO
         */
        public static PermisoResponse de(Permiso p) {
            return new PermisoResponse(p.getId(), p.getCodigo(), p.getModulo(), p.getDescripcion());
        }
    }

    /**
     * @param id          id
     * @param codigo      código
     * @param nombre      nombre
     * @param descripcion descripción
     * @param permisos    códigos de permisos
     */
    public record RolResponse(Long id, String codigo, String nombre, String descripcion, List<String> permisos) {

        /**
         * @param r entidad
         * @return DTO
         */
        public static RolResponse de(Rol r) {
            return new RolResponse(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion(),
                    r.getPermisos().stream().map(Permiso::getCodigo).sorted().toList());
        }
    }

    /**
     * @param permisos códigos de permisos asignados al rol
     */
    public record RolPermisosRequest(@NotEmpty(message = "{validacion.obligatorio}") List<String> permisos) {
    }
}
