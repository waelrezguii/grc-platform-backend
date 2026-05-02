package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.ChecklistResult;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_checklist_items", indexes = {
        @Index(name = "idx_aci_campaign", columnList = "campaign_id"),
        @Index(name = "idx_aci_result",   columnList = "result")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditChecklistItem extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AuditCampaign campaign;

    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "expected_evidence", columnDefinition = "TEXT")
    private String expectedEvidence;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChecklistResult result = ChecklistResult.NOT_EVALUATED;

    @Column(name = "actual_evidence", columnDefinition = "TEXT")
    private String actualEvidence;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
