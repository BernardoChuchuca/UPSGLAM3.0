package com.upsglam.repository;
import com.upsglam.model.Filter;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface FilterRepository extends ReactiveCrudRepository<Filter, UUID> {
    Flux<Filter> findByIsActiveTrue();
    Mono<Filter> findByName(String name);
}
