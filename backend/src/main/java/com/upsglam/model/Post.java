package com.upsglam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("posts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Post {
    @Id
    private UUID id;
    @Column("user_id")               private UUID userId;
    @Column("filter_id")             private UUID filterId;
    @Column("caption")               private String caption;
    @Column("original_image_url")    private String originalImageUrl;
    @Column("processed_image_url")   private String processedImageUrl;
    @Column("created_at")            private OffsetDateTime createdAt;
}
