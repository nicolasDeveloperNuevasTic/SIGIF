/*
 * Nombre: UsuarioController.java
 * Descripción: Endpoints REST de administración de usuarios, roles y permisos.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import com.sigif.usuario.UsuarioDtos.ApiKeyResponse;
import com.sigif.usuario.UsuarioDtos.CambioPasswordRequest;
import com.sigif.usuario.UsuarioDtos.EstadoUsuarioRequest;
import com.sigif.usuario.UsuarioDtos.PermisoResponse;
import com.sigif.usuario.UsuarioDtos.RolPermisosRequest;
import com.sigif.usuario.UsuarioDtos.RolResponse;
import com.sigif.usuario.UsuarioDtos.UsuarioRequest;
import com.sigif.usuario.UsuarioDtos.UsuarioResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración de seguridad RBAC (solo con permiso USUARIO_GESTIONAR).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USUARIO_GESTIONAR')")
public class UsuarioController {

    private final UsuarioService service;

    /**
     * @return usuarios
     */
    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    /**
     * @param req datos
     * @return usuario creado
     */
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
        return service.crear(req);
    }

    /**
     * @param id  id
     * @param req datos
     * @return usuario actualizado
     */
    @PutMapping("/usuarios/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return service.actualizar(id, req);
    }

    /**
     * @param id  id
     * @param req estado
     * @return usuario
     */
    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioResponse estado(@PathVariable Long id, @RequestBody EstadoUsuarioRequest req) {
        return service.cambiarEstado(id, req.activo());
    }

    /**
     * @param id  id
     * @param req nueva contraseña
     */
    @PatchMapping("/usuarios/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@PathVariable Long id, @Valid @RequestBody CambioPasswordRequest req) {
        service.cambiarPassword(id, req.password());
    }

    /**
     * @param id id del usuario de integración
     * @return API Key generada
     */
    @PostMapping("/usuarios/{id}/api-key")
    public ApiKeyResponse apiKey(@PathVariable Long id) {
        return service.generarApiKey(id);
    }

    /**
     * @return roles
     */
    @GetMapping("/roles")
    public List<RolResponse> roles() {
        return service.roles();
    }

    /**
     * @param id  id del rol
     * @param req permisos
     * @return rol actualizado
     */
    @PutMapping("/roles/{id}/permisos")
    public RolResponse permisosRol(@PathVariable Long id, @Valid @RequestBody RolPermisosRequest req) {
        return service.asignarPermisos(id, req.permisos());
    }

    /**
     * @return permisos
     */
    @GetMapping("/permisos")
    public List<PermisoResponse> permisos() {
        return service.permisos();
    }
}
