package com.starling.auth.model.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "wsi_edu", name = "case_curators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EduCaseCuratorEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "assigned_by")
    private UUID assignedBy;
}
