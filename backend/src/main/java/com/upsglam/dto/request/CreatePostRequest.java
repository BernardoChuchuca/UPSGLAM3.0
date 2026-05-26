package com.upsglam.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreatePostRequest {
    @NotBlank private String filterName;
    @Size(max = 300) private String caption;
}
