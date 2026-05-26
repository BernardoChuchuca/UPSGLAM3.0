package com.upsglam.dto.response;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Data @Builder
public class CommentResponse {
    private UUID   id;
    private String content;
    private ProfileResponse author;
    private OffsetDateTime createdAt;
}
