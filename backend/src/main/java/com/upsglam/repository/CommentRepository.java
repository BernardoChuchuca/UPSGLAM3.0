package com.upsglam.repository;
import com.upsglam.model.Comment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;
public interface CommentRepository extends ReactiveCrudRepository<Comment, UUID> {
    Flux<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
