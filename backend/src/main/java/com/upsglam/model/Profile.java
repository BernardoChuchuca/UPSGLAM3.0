// ─── Profile.java ───────────────────────────────────────────────
package com.upsglam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("profiles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Profile {
    @Id
    private UUID id;
    @Column("auth_user_id")  private UUID authUserId;
    @Column("username")      private String username;
    @Column("avatar_url")    private String avatarUrl;
    @Column("bio")           private String bio;
    @Column("created_at")    private OffsetDateTime createdAt;
}
