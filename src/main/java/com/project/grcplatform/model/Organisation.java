package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "organisations")
public class Organisation extends BaseEntity {

    private String code;
    private String name;
    private String description;
    private String phoneNumber;
    private Integer sortOrder;
    private String level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Organisation parent;
}
