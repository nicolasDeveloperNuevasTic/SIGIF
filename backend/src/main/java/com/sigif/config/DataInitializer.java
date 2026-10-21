/*
 * Nombre: DataInitializer.java
 * Descripción: Crea el usuario administrador inicial y, opcionalmente, usuarios de demostración por rol.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.config;

import com.sigif.usuario.RolRepository;
import com.sigif.usuario.Usuario;
import com.sigif.usuario.UsuarioRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las contraseñas se toman de variables de entorno y se guardan solo como hash BCrypt.
 * Los usuarios de demostración (contraseña {@code Sigif123*}) solo se crean si
 * {@code sigif.semilla.usuarios-demo=true}; en producción debe ser {@code false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Map<String, String> DEMO = Map.of(
            "bodega", "ROLE_INVENTORY_MANAGER",
            "compras", "ROLE_PURCHASING",
            "cajero", "ROLE_SALES",
            "auditor", "ROLE_AUDITOR",
            "integracion", "ROLE_INTEGRATION");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sigif.admin.usuario}")
    private String adminUsuario;

    @Value("${sigif.admin.password}")
    private String adminPassword;

    @Value("${sigif.admin.correo}")
    private String adminCorreo;

    @Value("${sigif.semilla.usuarios-demo:false}")
    private boolean usuariosDemo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crear(adminUsuario, adminPassword, "Administrador SIGIF", adminCorreo, "ROLE_ADMIN");
        if (usuariosDemo) {
            DEMO.forEach((usuario, rol) ->
                    crear(usuario, "Sigif123*", "Usuario demo " + usuario, usuario + "@sigif.local", rol));
        }
    }

    private void crear(String username, String password, String nombre, String correo, String rol) {
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        u.setRoles(new HashSet<>(Set.of(rolRepository.findByCodigo(rol).orElseThrow())));
        usuarioRepository.save(u);
        log.info("Usuario inicial creado: {} ({})", username, rol);
    }
}
