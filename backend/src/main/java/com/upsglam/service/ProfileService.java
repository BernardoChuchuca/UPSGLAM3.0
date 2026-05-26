package com.upsglam.service;
import com.upsglam.dto.request.UpdateProfileRequest;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepo;
    public Mono<ProfileResponse> getMyProfile(UUID authUserId) {
        return profileRepo.findByAuthUserId(authUserId).map(this::toResponse);
    }
    public Mono<ProfileResponse> getByUsername(String username) {
        return profileRepo.findByUsername(username).map(this::toResponse);
    }
    public Mono<ProfileResponse> updateProfile(UUID authUserId, UpdateProfileRequest req) {
        return profileRepo.findByAuthUserId(authUserId)
            .flatMap(p -> {
                if (req.getUsername() != null) p.setUsername(req.getUsername());
                if (req.getBio()      != null) p.setBio(req.getBio());
                if (req.getAvatarUrl()!= null) p.setAvatarUrl(req.getAvatarUrl());
                return profileRepo.save(p);
            }).map(this::toResponse);
    }
    private ProfileResponse toResponse(com.upsglam.model.Profile p) {
        return ProfileResponse.builder().id(p.getId()).username(p.getUsername())
            .avatarUrl(p.getAvatarUrl()).bio(p.getBio()).createdAt(p.getCreatedAt()).build();
    }
}
