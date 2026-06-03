package com.upsglam.controller;

import com.upsglam.dto.request.UpdateProfileRequest;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public Mono<ProfileResponse> getMe(Authentication auth) {
        return profileService.getMyProfile((UUID) auth.getPrincipal());
    }

    @GetMapping("/{username}")
    public Mono<ProfileResponse> getByUsername(@PathVariable String username, Authentication auth) {
        UUID authUserId = auth != null ? (UUID) auth.getPrincipal() : null;
        return profileService.getByUsername(username, authUserId);
    }

    @PatchMapping("/me")
    public Mono<ResponseEntity<ProfileResponse>> updateMe(
            @RequestBody UpdateProfileRequest req, Authentication auth) {
        return profileService.updateProfile((UUID) auth.getPrincipal(), req)
                .map(ResponseEntity::ok);
    }

    // ─── ENDPOINTS DE SEGUIMIENTO ────────────────────────────────

    @PostMapping("/{profileId}/follow")
    public Mono<ResponseEntity<Void>> followUser(@PathVariable UUID profileId, Authentication auth) {
        return profileService.followUser((UUID) auth.getPrincipal(), profileId)
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    @DeleteMapping("/{profileId}/follow")
    public Mono<ResponseEntity<Void>> unfollowUser(@PathVariable UUID profileId, Authentication auth) {
        return profileService.unfollowUser((UUID) auth.getPrincipal(), profileId)
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    @GetMapping("/{profileId}/followers")
    public Flux<ProfileResponse> getFollowers(@PathVariable UUID profileId, Authentication auth) {
        UUID authUserId = auth != null ? (UUID) auth.getPrincipal() : null;
        return profileService.getFollowers(profileId, authUserId);
    }

    @GetMapping("/{profileId}/following")
    public Flux<ProfileResponse> getFollowing(@PathVariable UUID profileId, Authentication auth) {
        UUID authUserId = auth != null ? (UUID) auth.getPrincipal() : null;
        return profileService.getFollowing(profileId, authUserId);
    }
}
