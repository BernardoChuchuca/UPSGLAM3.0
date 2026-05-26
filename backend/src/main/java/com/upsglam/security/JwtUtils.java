package com.upsglam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utilidades para validar y leer el JWT emitido por Supabase Auth.
 * Supabase firma con HS256 usando el JWT_SECRET del proyecto.
 */
@Component
public class JwtUtils {

    private final SecretKey secretKey;

    public JwtUtils(@Value("${supabase.jwt-secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valida el token y devuelve el UUID del usuario autenticado (claim "sub").
     * Lanza JwtException si el token es inválido o está expirado.
     */
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * Valida el token sin lanzar excepción — devuelve true si es válido.
     */
    public boolean isValid(String token) {
        try {
            extractUserId(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
