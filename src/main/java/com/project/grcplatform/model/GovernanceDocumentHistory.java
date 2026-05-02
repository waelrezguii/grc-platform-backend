package com.project.grcplatform.model;

import com.project.grcplatform.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "governance_document_history", indexes = {
        @Index(name = "idx_gdoc_history_doc_id", columnList = "document_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceDocumentHistory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private GovernanceDocument document;

    @Column(name = "change_summary")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot;
}