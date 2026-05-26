package com.upsglam.repository;
import com.upsglam.model.ProcessingHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;
public interface ProcessingHistoryRepository extends ReactiveCrudRepository<ProcessingHistory, UUID> {
    Flux<ProcessingHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
