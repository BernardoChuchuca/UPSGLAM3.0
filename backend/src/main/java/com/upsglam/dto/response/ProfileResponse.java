package com.upsglam.dto.response;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Data @Builder
public class ProfileResponse {
    private UUID   id;
    private String username;
    private String avatarUrl;
    private String bio;
    private OffsetDateTime createdAt;
}
