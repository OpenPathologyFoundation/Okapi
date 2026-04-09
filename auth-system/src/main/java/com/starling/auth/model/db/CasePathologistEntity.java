package com.starling.auth.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "wsi", name = "case_pathologists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasePathologistEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "designation", nullable = false, length = 32)
    private String designation;

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 1;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "assigned_by")
    private UUID assignedBy;
}
