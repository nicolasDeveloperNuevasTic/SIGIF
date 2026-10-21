/*
 * Nombre: UsuarioService.java
 * Descripción: Lógica de autenticación y administración de usuarios, roles y permisos (RBAC).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import com.sigif.auditoria.AuditoriaService;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.excepcion.ReglaNegocioException;
import com.sigif.security.ApiKeyAuthenticationFilter;
import com.sigif.security.JwtService;
import com.sigif.usuario.UsuarioDtos.ApiKeyResponse;
import com.sigif.usuario.UsuarioDtos.LoginResponse;
import com.sigif.usuario.UsuarioDtos.PermisoResponse;
import com.sigif.usuario.UsuarioDtos.RolResponse;
import com.sigif.usuario.UsuarioDtos.UsuarioRequest;
import com.sigif.usuario.UsuarioDtos.UsuarioResponse;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de seguridad: login con BCrypt, gestión de usuarios y asignación de permisos a roles.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    /**
     * Autentica al usuario y emite un JWT.
     *
     * @param username usuario
     * @param password contraseña en texto plano
     * @return token y datos del usuario
     */
    @Transactional
    public LoginResponse login(String username, String password) {
        Usuario u = usuarioRepository.findByUsernameIgnoreCase(username)
                .filter(x -> passwordEncoder.matches(password, x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("credenciales"));
        if (!u.isActivo()) {
            throw new DisabledException("inactivo");
        }
        u.setUltimoAcceso(OffsetDateTime.now());
        auditoriaService.registrarEventoSeguridad("LOGIN", u.getUsername(), "Inicio de sesión");
        return new LoginResponse(jwtService.generar(u.getUsername(), u.autoridades()), "Bearer",
                jwtService.expiracionSegundos(), UsuarioResponse.de(u));
    }

    /**
     * @param username usuario autenticado
     * @return perfil
     */
    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username).map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("recurso.usuario", username));
    }

    /**
     * @return todos los usuarios
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("username")).stream().map(UsuarioResponse::de).toList();
    }

    /**
     * @param req datos del usuario (password obligatorio)
     * @return usuario creado
     */
    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new ReglaNegocioException("validacion.password");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new ConflictoException("usuario.username.duplicado", req.username());
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(req.correo())) {
            throw new ConflictoException("usuario.correo.duplicado", req.correo());
        }
        Usuario u = new Usuario();
        u.setUsername(req.username().trim().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        aplicar(u, req);
        return UsuarioResponse.de(usuarioRepository.save(u));
    }

    /**
     * @param id  id del usuario
     * @param req datos (password se ignora)
     * @return usuario actualizado
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req) {
        Usuario u = obtener(id);
        if (!u.getCorreo().equalsIgnoreCase(req.correo()) && usuarioRepository.existsByCorreoIgnoreCase(req.correo())) {
            throw new ConflictoException("usuario.correo.duplicado", req.correo());
        }
        aplicar(u, req);
        return UsuarioResponse.de(u);
    }

    /**
     * @param id     id
     * @param activo nuevo estado
     * @return usuario
     */
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario u = obtener(id);
        u.setActivo(activo);
        return UsuarioResponse.de(u);
    }

    /**
     * @param id       id
     * @param password nueva contraseña
     */
    @Transactional
    public void cambiarPassword(Long id, String password) {
        obtener(id).setPasswordHash(passwordEncoder.encode(password));
    }

    /**
     * Genera una API Key para integración M2M; solo se guarda su hash SHA-256.
     *
     * @param id id del usuario (debe tener ROLE_INTEGRATION)
     * @return API Key en texto plano
     */
    @Transactional
    public ApiKeyResponse generarApiKey(Long id) {
        Usuario u = obtener(id);
        if (u.getRoles().stream().noneMatch(r -> "ROLE_INTEGRATION".equals(r.getCodigo()))) {
            throw new ReglaNegocioException("usuario.apikey.rol");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String clave = "sigif_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        u.setApiKeyHash(ApiKeyAuthenticationFilter.hash(clave));
        return new ApiKeyResponse(clave);
    }

    /**
     * @return roles con sus permisos
     */
    @Transactional(readOnly = true)
    public List<RolResponse> roles() {
        return rolRepository.findAll(Sort.by("codigo")).stream().map(RolResponse::de).toList();
    }

    /**
     * @return catálogo de permisos
     */
    @Transactional(readOnly = true)
    public List<PermisoResponse> permisos() {
        return permisoRepository.findAll(Sort.by("modulo", "codigo")).stream().map(PermisoResponse::de).toList();
    }

    /**
     * @param rolId    id del rol
     * @param permisos códigos de permisos
     * @return rol actualizado
     */
    @Transactional
    public RolResponse asignarPermisos(Long rolId, List<String> permisos) {
        Rol rol = rolRepository.findById(rolId).orElseThrow(() -> new RecursoNoEncontradoException("recurso.rol", rolId));
        List<Permiso> encontrados = permisoRepository.findByCodigoIn(permisos);
        if (encontrados.size() != new HashSet<>(permisos).size()) {
            throw new ReglaNegocioException("rol.permiso.invalido");
        }
        rol.setPermisos(new HashSet<>(encontrados));
        return RolResponse.de(rol);
    }

    private void aplicar(Usuario u, UsuarioRequest req) {
        u.setNombreCompleto(req.nombreCompleto().trim());
        u.setCorreo(req.correo().trim().toLowerCase());
        u.setIdioma(req.idioma() == null ? "es" : req.idioma());
        List<Rol> roles = rolRepository.findByCodigoIn(req.roles());
        if (roles.size() != new HashSet<>(req.roles()).size()) {
            throw new ReglaNegocioException("usuario.rol.invalido");
        }
        u.setRoles(new HashSet<>(roles));
    }

    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("recurso.usuario", id));
    }
}
