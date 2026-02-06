package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pathology_worklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorklistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Core identification
    @Column(name = "accession_number", nullable = false, unique = true)
    private String accessionNumber;

    @Column(name = "patient_mrn")
    private String patientMrn;

    @Column(name = "patient_display")
    private String patientDisplay;

    // Service and specimen
    @Column(name = "service", nullable = false)
    private String service;

    @Column(name = "specimen_type")
    private String specimenType;

    @Column(name = "specimen_site")
    private String specimenSite;

    // Status tuple
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACCESSIONED";

    @Column(name = "lis_status")
    private String lisStatus;

    @Column(name = "wsi_status")
    private String wsiStatus;

    @Column(name = "authoring_status")
    private String authoringStatus;

    // Priority
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private String priority = "ROUTINE";

    // Assignment
    @Column(name = "assigned_to_identity_id")
    private UUID assignedToIdentityId;

    @Column(name = "assigned_to_display")
    private String assignedToDisplay;

    // Slide counts
    @Column(name = "slide_count", nullable = false)
    @Builder.Default
    private Integer slideCount = 0;

    @Column(name = "slide_pending", nullable = false)
    @Builder.Default
    private Integer slidePending = 0;

    @Column(name = "slide_scanned", nullable = false)
    @Builder.Default
    private Integer slideScanned = 0;

    // Timestamps
    @Column(name = "case_date", nullable = false)
    @Builder.Default
    private LocalDate caseDate = LocalDate.now();

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private OffsetDateTime receivedAt = OffsetDateTime.now();

    @Column(name = "collected_at")
    private OffsetDateTime collectedAt;

    // Enrichment (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> annotations = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alerts", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> alerts = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    // Audit
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
