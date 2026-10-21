/*
 * Nombre: ApiKeyAuthenticationFilter.java
 * Descripción: Filtro de autenticación M2M para sistemas externos mediante X-API-Key.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.security;

import com.sigif.usuario.Usuario;
import com.sigif.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica sistemas externos (ACT-09, ROLE_INTEGRATION) comparando el hash SHA-256 de la
 * cabecera {@code X-API-Key} con el almacenado en el usuario de integración.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    /** Cabecera de la API Key. */
    public static final String CABECERA = "X-API-Key";

    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(CABECERA);
        if (apiKey != null && !apiKey.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            usuarioRepository.findByApiKeyHash(hash(apiKey)).filter(Usuario::isActivo).ifPresent(u -> {
                var autoridades = u.autoridades().stream().map(SimpleGrantedAuthority::new).toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(u.getUsername(), null, autoridades));
            });
        }
        chain.doFilter(request, response);
    }

    /**
     * @param valor texto plano
     * @return hash SHA-256 en hexadecimal
     */
    public static String hash(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
