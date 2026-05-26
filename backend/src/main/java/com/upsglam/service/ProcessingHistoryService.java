package com.upsglam.service;
import com.upsglam.dto.response.ProcessingHistoryResponse;
import com.upsglam.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class ProcessingHistoryService {
    private final ProcessingHistoryRepository historyRepo;
    private final GpuMetricsRepository metricsRepo;
    private final FilterRepository filterRepo;
    private final ProfileRepository profileRepo;
    public Flux<ProcessingHistoryResponse> getMyHistory(UUID authUserId) {
        return profileRepo.findByAuthUserId(authUserId)
            .flatMapMany(p -> historyRepo.findByUserIdOrderByCreatedAtDesc(p.getId()))
            .flatMap(h -> metricsRepo.findByProcessingId(h.getId())
                .zipWith(filterRepo.findById(h.getFilterId()))
                .map(t -> ProcessingHistoryResponse.builder()
                    .id(h.getId()).filterName(t.getT2().getName())
                    .originalImageUrl(h.getOriginalImageUrl())
                    .processedImageUrl(h.getProcessedImageUrl())
                    .status(h.getStatus())
                    .blockDim(t.getT1().getBlockDim()).gridDim(t.getT1().getGridDim())
                    .totalThreads(t.getT1().getTotalThreads())
                    .kernelTimeMs(t.getT1().getKernelTimeMs())
                    .imageWidth(t.getT1().getImageWidth()).imageHeight(t.getT1().getImageHeight())
                    .createdAt(h.getCreatedAt()).build()));
    }
}
