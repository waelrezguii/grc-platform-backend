package com.project.grcplatform.model;

import com.project.grcplatform.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "audit_recommendation_history", indexes = {
        @Index(name = "idx_ar_history_rec_id", columnList = "recommendation_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditRecommendationHistory extends AuditableEntity {

    @Column(name = "recommendation_id", nullable = false)
    private String recommendationId;

    @Column(name = "change_summary")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot;
}
