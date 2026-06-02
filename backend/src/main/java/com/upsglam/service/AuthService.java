package com.upsglam.service;

import com.upsglam.dto.request.LoginRequest;
import com.upsglam.dto.request.RegisterRequest;
import com.upsglam.dto.response.AuthResponse;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.model.Profile;
import com.upsglam.repository.ProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final ProfileRepository profileRepo;
    private final WebClient authWebClient;

    public AuthService(
            ProfileRepository profileRepo,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String anonKey) {
        this.profileRepo = profileRepo;
        this.authWebClient = WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", anonKey)
                .defaultHeader("Authorization", "Bearer " + anonKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Registra un nuevo usuario en Supabase Auth y crea su perfil en la base de datos local.
     */
    public Mono<ProfileResponse> register(RegisterRequest req) {
        return profileRepo.existsByUsername(req.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "El nombre de usuario ya está en uso"));
                    }

                    Map<String, Object> body = Map.of(
                            "email", req.getEmail(),
                            "password", req.getPassword(),
                            "data", Map.of("username", req.getUsername())
                    );

                    log.info("Iniciando registro para email: {} con username: {}", req.getEmail(), req.getUsername());

                    return authWebClient.post()
                            .uri("/auth/v1/signup")
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(status -> status.isError(), clientResponse ->
                                    clientResponse.bodyToMono(Map.class)
                                            .flatMap(errBody -> {
                                                log.error("Error en registro Supabase: {}", errBody);
                                                String msg = errBody.containsKey("msg") ? (String) errBody.get("msg")
                                                        : (errBody.containsKey("message") ? (String) errBody.get("message")
                                                        : "Error al registrar en Supabase");
                                                return Mono.error(new ResponseStatusException(clientResponse.statusCode(), msg));
                                            })
                            )
                            .bodyToMono(Map.class)
                            .flatMap(res -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> userMap = (Map<String, Object>) res.get("user");
                                if (userMap == null) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.INTERNAL_SERVER_ERROR, "Respuesta de registro inválida de Supabase Auth (falta objeto user)"));
                                }
                                String authUserIdStr = (String) userMap.get("id");
                                if (authUserIdStr == null) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.INTERNAL_SERVER_ERROR, "Respuesta de registro inválida de Supabase Auth (falta id de usuario)"));
                                }
                                UUID authUserId = UUID.fromString(authUserIdStr);

                                // Recuperar el perfil creado automáticamente por el trigger de Supabase
                                return profileRepo.findByAuthUserId(authUserId);
                            })
                            .map(p -> ProfileResponse.builder()
                                    .id(p.getId())
                                    .username(p.getUsername())
                                    .avatarUrl(p.getAvatarUrl())
                                    .bio(p.getBio())
                                    .createdAt(p.getCreatedAt())
                                    .build());
                });
    }

    /**
     * Inicia sesión de un usuario en Supabase Auth y devuelve su JWT y perfil local.
     */
    @SuppressWarnings("unchecked")
    public Mono<AuthResponse> login(LoginRequest req) {
        Map<String, Object> body = Map.of(
                "email", req.getEmail(),
                "password", req.getPassword()
        );

        log.info("Iniciando sesión para email: {}", req.getEmail());

        return authWebClient.post()
                .uri("/auth/v1/token?grant_type=password")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(Map.class)
                                .flatMap(errBody -> {
                                    log.error("Error en login Supabase: {}", errBody);
                                    String msg = errBody.containsKey("error_description") ? (String) errBody.get("error_description")
                                            : (errBody.containsKey("msg") ? (String) errBody.get("msg")
                                            : "Credenciales inválidas");
                                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), msg));
                                })
                )
                .bodyToMono(Map.class)
                .flatMap(res -> {
                    String accessToken = (String) res.get("access_token");
                    Map<String, Object> userMap = (Map<String, Object>) res.get("user");
                    if (accessToken == null || userMap == null) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "Respuesta de login inválida de Supabase Auth"));
                    }
                    String authUserIdStr = (String) userMap.get("id");
                    UUID authUserId = UUID.fromString(authUserIdStr);

                    return profileRepo.findByAuthUserId(authUserId)
                            .map(p -> AuthResponse.builder()
                                    .token(accessToken)
                                    .profile(ProfileResponse.builder()
                                            .id(p.getId())
                                            .username(p.getUsername())
                                            .avatarUrl(p.getAvatarUrl())
                                            .bio(p.getBio())
                                            .createdAt(p.getCreatedAt())
                                            .build())
                                    .build())
                            .switchIfEmpty(
                                    // Crear perfil por defecto si no existe localmente
                                    Mono.defer(() -> {
                                        String provisionalUsername = req.getEmail().split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
                                        log.warn("Perfil local no encontrado para authUserId: {}. Creando perfil provisional con username: {}", authUserId, provisionalUsername);
                                        Profile newProfile = Profile.builder()
                                                .authUserId(authUserId)
                                                .username(provisionalUsername)
                                                .createdAt(OffsetDateTime.now())
                                                .build();
                                        return profileRepo.save(newProfile)
                                                .map(p -> AuthResponse.builder()
                                                        .token(accessToken)
                                                        .profile(ProfileResponse.builder()
                                                                .id(p.getId())
                                                                .username(p.getUsername())
                                                                .avatarUrl(p.getAvatarUrl())
                                                                .bio(p.getBio())
                                                                .createdAt(p.getCreatedAt())
                                                                .build())
                                                        .build());
                                    })
                            );
                });
    }

    /**
     * Cierra la sesión del usuario en Supabase Auth invalidando su JWT.
     */
    public Mono<Void> logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de autorización inválido"));
        }
        String userToken = authHeader.substring(7);

        log.info("Cerrando sesión en Supabase...");

        return authWebClient.post()
                .uri("/auth/v1/logout")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(Map.class)
                                .flatMap(errBody -> {
                                    log.error("Error en logout Supabase: {}", errBody);
                                    String msg = errBody.containsKey("msg") ? (String) errBody.get("msg")
                                            : "Error al cerrar sesión en Supabase";
                                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), msg));
                                })
                )
                .toBodilessEntity()
                .then();
    }
}
