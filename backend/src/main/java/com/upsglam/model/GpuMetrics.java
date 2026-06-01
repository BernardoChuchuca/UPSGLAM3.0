package com.upsglam.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Table("gpu_metrics")
public class GpuMetrics {
    @Id
    private UUID id;
    
    @Column("history_id")
    private UUID historyId;

    @Column("processing_id")
    private UUID processingId;
    
    @Column("execution_time_cpu")
    private BigDecimal executionTimeCpu;
    
    @Column("execution_time_gpu")
    private BigDecimal executionTimeGpu;
    
    @Column("speedup")
    private BigDecimal speedup;

    @Column("block_dim")
    private String blockDim;

    @Column("grid_dim")
    private String gridDim;

    @Column("total_threads")
    private Long totalThreads; 

    @Column("kernel_time_ms")
    private BigDecimal kernelTimeMs;

    @Column("image_width")
    private Integer imageWidth;

    @Column("image_height")
    private Integer imageHeight;
    
    @Column("created_at")
    private OffsetDateTime createdAt;

    // --- TRUCO DE CONVERSIÓN EN CALIENTE PARA LOS GETTERS ---
    // Si el servicio llama a getTotalThreads() esperando un Integer, este método romperá el error devolviendo el entero directamente.
    public Integer getTotalThreads() {
        return totalThreads != null ? totalThreads.intValue() : null;
    }

    // --- TRUCO DE SOBRECARGA COMPLETO PARA EL BUILDER ---
    public static class GpuMetricsBuilder {
        public GpuMetricsBuilder blockDim(Integer blockDim) {
            this.blockDim = blockDim != null ? blockDim.toString() : null;
            return this;
        }
        public GpuMetricsBuilder blockDim(String blockDim) {
            this.blockDim = blockDim;
            return this;
        }

        public GpuMetricsBuilder gridDim(Integer gridDim) {
            this.gridDim = gridDim != null ? gridDim.toString() : null;
            return this;
        }
        public GpuMetricsBuilder gridDim(String gridDim) {
            this.gridDim = gridDim;
            return this;
        }

        public GpuMetricsBuilder totalThreads(Integer totalThreads) {
            this.totalThreads = totalThreads != null ? totalThreads.longValue() : null;
            return this;
        }
        public GpuMetricsBuilder totalThreads(Long totalThreads) {
            this.totalThreads = totalThreads;
            return this;
        }
    }
}