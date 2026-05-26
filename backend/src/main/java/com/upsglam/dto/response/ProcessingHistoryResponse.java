package com.upsglam.dto.response;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@Data @Builder
public class ProcessingHistoryResponse {
    private UUID   id;
    private String filterName;
    private String originalImageUrl;
    private String processedImageUrl;
    private String status;
    private String blockDim;
    private String gridDim;
    private Integer totalThreads;
    private BigDecimal kernelTimeMs;
    private Integer imageWidth;
    private Integer imageHeight;
    private OffsetDateTime createdAt;
}
