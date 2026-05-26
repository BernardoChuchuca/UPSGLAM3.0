package com.upsglam.repository;
import com.upsglam.model.Post;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;
public interface PostRepository extends ReactiveCrudRepository<Post, UUID> {
    Flux<Post> findAllByOrderByCreatedAtDesc();
    Flux<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
