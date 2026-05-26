package com.upsglam.dto.request;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class UpdateProfileRequest {
    @Size(min = 3, max = 50) private String username;
    @Size(max = 200)         private String bio;
    private String avatarUrl;
}
