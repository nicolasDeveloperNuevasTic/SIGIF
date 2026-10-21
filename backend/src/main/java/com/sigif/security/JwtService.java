/*
 * Nombre: JwtService.java
 * Descripción: Emisión y validación de tokens JWT firmados (HS256).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Genera tokens con el usuario y sus autoridades (roles + permisos) y los valida.
 */
@Component
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    /**
     * @param secreto           secreto en Base64 (mínimo 256 bits)
     * @param expiracionMinutos vigencia del token
     */
    public JwtService(@Value("${sigif.seguridad.jwt-secreto}") String secreto,
                      @Value("${sigif.seguridad.jwt-expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        this.expiracionMinutos = expiracionMinutos;
    }

    /**
     * @param username    usuario
     * @param autoridades roles y permisos
     * @return token JWT firmado
     */
    public String generar(String username, Collection<String> autoridades) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("aut", List.copyOf(autoridades))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /**
     * @param token token recibido
     * @return claims si el token es válido y vigente
     */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * @return vigencia en segundos
     */
    public long expiracionSegundos() {
        return expiracionMinutos * 60;
    }
}
