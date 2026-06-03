package com.upsglam.repository;

import com.upsglam.model.Follow;
import com.upsglam.model.Profile;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface FollowRepository extends ReactiveCrudRepository<Follow, UUID> {
    Mono<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    Mono<Boolean> existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    Mono<Long> countByFollowerId(UUID followerId);
    Mono<Long> countByFollowingId(UUID followingId);
    
    @Query("SELECT p.* FROM profiles p JOIN follows f ON p.id = f.follower_id WHERE f.following_id = :followingId")
    Flux<Profile> findFollowersOf(UUID followingId);

    @Query("SELECT p.* FROM profiles p JOIN follows f ON p.id = f.following_id WHERE f.follower_id = :followerId")
    Flux<Profile> findFollowingOf(UUID followerId);
}
