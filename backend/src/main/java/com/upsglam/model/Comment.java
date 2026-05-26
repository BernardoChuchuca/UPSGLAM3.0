package com.upsglam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("comments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Comment {
    @Id
    private UUID id;
    @Column("post_id")    private UUID postId;
    @Column("user_id")    private UUID userId;
    @Column("content")    private String content;
    @Column("created_at") private OffsetDateTime createdAt;
}
