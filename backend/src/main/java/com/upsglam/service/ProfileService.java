package com.upsglam.service;

import com.upsglam.dto.request.UpdateProfileRequest;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.model.Follow;
import com.upsglam.repository.FollowRepository;
import com.upsglam.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepo;
    private final FollowRepository followRepo;

    public Mono<ProfileResponse> getMyProfile(UUID authUserId) {
        return profileRepo.findByAuthUserId(authUserId)
                .flatMap(p -> enrichProfileResponse(p, authUserId));
    }

    public Mono<ProfileResponse> getByUsername(String username, UUID authUserId) {
        return profileRepo.findByUsername(username)
                .flatMap(p -> enrichProfileResponse(p, authUserId));
    }

    public Mono<ProfileResponse> updateProfile(UUID authUserId, UpdateProfileRequest req) {
        return profileRepo.findByAuthUserId(authUserId)
                .flatMap(p -> {
                    if (req.getUsername() != null) p.setUsername(req.getUsername());
                    if (req.getBio()      != null) p.setBio(req.getBio());
                    if (req.getAvatarUrl()!= null) p.setAvatarUrl(req.getAvatarUrl());
                    return profileRepo.save(p);
                })
                .flatMap(p -> enrichProfileResponse(p, authUserId));
    }

    // ─── ACCIONES DE SEGUIMIENTO ─────────────────────────────────

    public Mono<Void> followUser(UUID authUserId, UUID followingProfileId) {
        return profileRepo.findByAuthUserId(authUserId)
                .flatMap(me -> {
                    if (me.getId().equals(followingProfileId)) {
                        return Mono.error(new IllegalArgumentException("No puedes seguirte a ti mismo"));
                    }
                    return followRepo.findByFollowerIdAndFollowingId(me.getId(), followingProfileId)
                            .switchIfEmpty(Mono.defer(() -> followRepo.save(
                                    Follow.builder()
                                            .followerId(me.getId())
                                            .followingId(followingProfileId)
                                            .createdAt(OffsetDateTime.now())
                                            .build()
                            )))
                            .then();
                });
    }

    public Mono<Void> unfollowUser(UUID authUserId, UUID followingProfileId) {
        return profileRepo.findByAuthUserId(authUserId)
                .flatMap(me -> followRepo.findByFollowerIdAndFollowingId(me.getId(), followingProfileId)
                        .flatMap(followRepo::delete)
                );
    }

    public Flux<ProfileResponse> getFollowers(UUID profileId, UUID authUserId) {
        return followRepo.findFollowersOf(profileId)
                .flatMap(p -> enrichProfileResponse(p, authUserId));
    }

    public Flux<ProfileResponse> getFollowing(UUID profileId, UUID authUserId) {
        return followRepo.findFollowingOf(profileId)
                .flatMap(p -> enrichProfileResponse(p, authUserId));
    }

    // ─── HELPERS ─────────────────────────────────────────────────

    private Mono<ProfileResponse> enrichProfileResponse(com.upsglam.model.Profile p, UUID authUserId) {
        Mono<Long> followersMono = followRepo.countByFollowingId(p.getId());
        Mono<Long> followingMono = followRepo.countByFollowerId(p.getId());
        
        Mono<Boolean> followedByMeMono = authUserId == null
                ? Mono.just(false)
                : profileRepo.findByAuthUserId(authUserId)
                    .flatMap(me -> followRepo.existsByFollowerIdAndFollowingId(me.getId(), p.getId()))
                    .defaultIfEmpty(false);

        return Mono.zip(followersMono, followingMono, followedByMeMono)
                .map(t -> ProfileResponse.builder()
                        .id(p.getId())
                        .username(p.getUsername())
                        .avatarUrl(p.getAvatarUrl())
                        .bio(p.getBio())
                        .createdAt(p.getCreatedAt())
                        .followersCount(t.getT1())
                        .followingCount(t.getT2())
                        .followedByMe(t.getT3())
                        .build());
    }
}
