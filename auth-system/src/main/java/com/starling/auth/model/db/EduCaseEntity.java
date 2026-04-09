package com.starling.auth.model.db;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "wsi_edu", name = "cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EduCaseEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "collection", nullable = false)
    @Builder.Default
    private String collection = "educational";

    @Column(name = "specimen_type")
    private String specimenType;

    @Column(name = "clinical_history")
    private String clinicalHistory;

    @Column(name = "accession_date")
    private LocalDate accessionDate;

    @Column(name = "ingested_at", nullable = false)
    @Builder.Default
    private OffsetDateTime ingestedAt = OffsetDateTime.now();

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "active";

    @Column(name = "priority")
    private String priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_lineage")
    private Map<String, Object> sourceLineage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
