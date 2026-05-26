package com.upsglam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("filters")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Filter {
    @Id
    private UUID id;
    @Column("name")        private String name;
    @Column("description") private String description;
    @Column("is_active")   private Boolean isActive;
    @Column("created_at")  private OffsetDateTime createdAt;
}
