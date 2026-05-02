package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_campaign_history", indexes = {
        @Index(name = "idx_ac_history_campaign_id", columnList = "campaign_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditCampaignHistory extends BaseEntity {
    
    //! NOTE : WORK NOT COMPLETED !!!!

    //! BIND With CAMPAIGN Table (if NEEDED !!)
    @Column(name = "campaign_id", nullable = false)
    private String campaignId;

    //! BIND With USER Table (if NEEDED !!)
    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "change_summary")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot;

    //! Use Annotation
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * 
     * 
     *  THE FIX For Timestamp:
     * 
     * 
     * 
            @CreationTimestamp
            @Column(name = "changed_at", updatable = false)
            private LocalDateTime changedAt;

     */

    //! TO BE REMOVED
    @PrePersist
    public void onCreate() {
        if (this.changedAt == null) this.changedAt = LocalDateTime.now();
    }
    
}
