package com.upsglam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("follows")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Follow {
    @Id
    private UUID id;
    @Column("follower_id")  private UUID followerId;
    @Column("following_id") private UUID followingId;
    @Column("created_at")   private OffsetDateTime createdAt;
}
