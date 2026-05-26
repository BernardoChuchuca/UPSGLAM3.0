package com.upsglam.repository;
import com.upsglam.model.Like;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface LikeRepository extends ReactiveCrudRepository<Like, UUID> {
    Mono<Like>    findByPostIdAndUserId(UUID postId, UUID userId);
    Mono<Long>    countByPostId(UUID postId);
    Mono<Boolean> existsByPostIdAndUserId(UUID postId, UUID userId);
}
