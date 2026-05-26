package com.upsglam.model;
<<<<<<< HEAD

=======
>>>>>>> 661659fe00c77f5d8cfa7b89579d9480e250b2ad
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("gpu_metrics")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GpuMetrics {
    @Id
    private UUID id;
    @Column("processing_id")   private UUID processingId;
    @Column("block_dim")       private String blockDim;
    @Column("grid_dim")        private String gridDim;
    @Column("total_threads")   private Integer totalThreads;
    @Column("kernel_time_ms")  private BigDecimal kernelTimeMs;
    @Column("image_width")     private Integer imageWidth;
    @Column("image_height")    private Integer imageHeight;
    @Column("created_at")      private OffsetDateTime createdAt;
}
