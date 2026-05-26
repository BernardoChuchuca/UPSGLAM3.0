package com.upsglam.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateCommentRequest {
    @NotBlank @Size(min = 1, max = 500) private String content;
}
