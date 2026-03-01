package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity for wsi.cases — used for case validation, accession resolution,
 * and worklist sync enrichment.
 */
@Entity
@Table(schema = "wsi", name = "cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "collection", nullable = false)
    private String collection;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "specimen_type")
    private String specimenType;

    @Column(name = "accession_date")
    private LocalDate accessionDate;

    @Column(name = "priority")
    private String priority;

    @Column(name = "patient_id")
    private UUID patientId;
}
