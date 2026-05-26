package com.upsglam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("processing_history")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProcessingHistory {
    @Id
    private UUID id;
    @Column("user_id")               private UUID userId;
    @Column("post_id")               private UUID postId;
    @Column("filter_id")             private UUID filterId;
    @Column("original_image_url")    private String originalImageUrl;
    @Column("processed_image_url")   private String processedImageUrl;
    @Column("status")                private String status;   // pending | success | error
    @Column("created_at")            private OffsetDateTime createdAt;
}
