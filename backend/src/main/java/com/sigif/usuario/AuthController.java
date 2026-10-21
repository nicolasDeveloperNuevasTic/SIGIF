/*
 * Nombre: AuthController.java
 * Descripción: Endpoints de autenticación (login JWT y perfil del usuario actual).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.usuario;

import com.sigif.common.util.UsuarioActual;
import com.sigif.usuario.UsuarioDtos.LoginRequest;
import com.sigif.usuario.UsuarioDtos.LoginResponse;
import com.sigif.usuario.UsuarioDtos.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación de usuarios del Front-End.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService service;

    /**
     * @param req credenciales
     * @return token JWT
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req.username(), req.password());
    }

    /**
     * @return perfil y autoridades del usuario autenticado
     */
    @GetMapping("/me")
    public UsuarioResponse perfil() {
        return service.perfil(UsuarioActual.nombre());
    }
}
