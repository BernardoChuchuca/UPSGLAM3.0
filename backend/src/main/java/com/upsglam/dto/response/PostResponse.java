package com.upsglam.dto.response;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class PostResponse {
    private UUID   id;
    private String caption;
    private String originalImageUrl;
    private String processedImageUrl;
    private String filterName;
    private ProfileResponse author;
    private Long   likeCount;
    private boolean likedByMe;
    private OffsetDateTime createdAt;
    
    // Nuevos campos para reposteos
    private Long repostCount;
    private boolean repostedByMe;
}
