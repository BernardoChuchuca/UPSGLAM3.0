package com.upsglam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Utilidades para validar y leer el JWT emitido por Supabase Auth.
 * Supabase firma con HS256 usando el JWT_SECRET del proyecto.
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;

    public JwtUtils(@Value("${supabase.jwt-secret}") String jwtSecret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret.trim());
            log.info("JWT Secret successfully decoded from Base64");
        } catch (IllegalArgumentException e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            log.warn("JWT Secret could not be decoded as Base64, using raw bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
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
            log.error("JWT Validation failed: {}", e.getMessage());
            return false;
        }
    }
}
