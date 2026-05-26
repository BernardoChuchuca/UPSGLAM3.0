package com.upsglam.controller;
import com.upsglam.dto.request.UpdateProfileRequest;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.UUID;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    @GetMapping("/me")
    public Mono<ProfileResponse> getMe(Authentication auth) {
        return profileService.getMyProfile((UUID) auth.getPrincipal());
    }
    @GetMapping("/{username}")
    public Mono<ProfileResponse> getByUsername(@PathVariable String username) {
        return profileService.getByUsername(username);
    }
    @PatchMapping("/me")
    public Mono<ResponseEntity<ProfileResponse>> updateMe(
            @RequestBody UpdateProfileRequest req, Authentication auth) {
        return profileService.updateProfile((UUID) auth.getPrincipal(), req)
            .map(ResponseEntity::ok);
    }
}
