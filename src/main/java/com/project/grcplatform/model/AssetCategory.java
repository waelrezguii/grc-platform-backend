package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
    @Table(name = "asset_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetCategory extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
