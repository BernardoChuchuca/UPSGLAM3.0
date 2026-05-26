package com.upsglam.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CudaProcessingResponse {
    @JsonProperty("filter_name")    private String filterName;
    @JsonProperty("width")          private Integer width;
    @JsonProperty("height")         private Integer height;
    @JsonProperty("block_dim")      private String blockDim;
    @JsonProperty("grid_dim")       private String gridDim;
    @JsonProperty("total_threads")  private Integer totalThreads;
    @JsonProperty("kernel_time_ms") private BigDecimal kernelTimeMs;
    @JsonProperty("status")         private String status;
    @JsonProperty("image_base64")   private String imageBase64;
}
