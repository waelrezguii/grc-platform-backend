package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_grades")
public class UserGrade extends BaseEntity {

    private String name;
    private String description;
    private Integer orderLevel;
}
