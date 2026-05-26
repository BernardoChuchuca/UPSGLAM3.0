package com.upsglam.repository;
import com.upsglam.model.GpuMetrics;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface GpuMetricsRepository extends ReactiveCrudRepository<GpuMetrics, UUID> {
    Mono<GpuMetrics> findByProcessingId(UUID processingId);
}
