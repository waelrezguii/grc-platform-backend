package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "governance_documents", indexes = {
        @Index(name = "idx_gdoc_status",        columnList = "status"),
        @Index(name = "idx_gdoc_owner",         columnList = "owner_id"),
        @Index(name = "idx_gdoc_linked_policy", columnList = "linked_policy_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceDocument extends SoftDeleteEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    private String version;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_policy_id")
    private GovernancePolicy linkedPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
