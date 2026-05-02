package com.project.grcplatform.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeleteEntity extends AuditableEntity {

    @Column(nullable = false)
    private Boolean deleted = false;

}