package com.upsglam.repository;
import com.upsglam.model.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface ProfileRepository extends ReactiveCrudRepository<Profile, UUID> {
    Mono<Profile> findByAuthUserId(UUID authUserId);
    Mono<Profile> findByUsername(String username);
    Mono<Boolean> existsByUsername(String username);
}
