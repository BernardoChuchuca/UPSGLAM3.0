package com.upsglam.repository;

import com.upsglam.model.Repost;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface RepostRepository extends ReactiveCrudRepository<Repost, UUID> {
    Mono<Repost> findByPostIdAndUserId(UUID postId, UUID userId);
    Mono<Boolean> existsByPostIdAndUserId(UUID postId, UUID userId);
    Mono<Long> countByPostId(UUID postId);
    Flux<Repost> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
