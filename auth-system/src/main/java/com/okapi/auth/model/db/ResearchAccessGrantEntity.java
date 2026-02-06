package com.okapi.auth.model.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "iam", name = "research_access_grant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchAccessGrantEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "grant_id")
    private UUID grantId;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "scope_type", nullable = false)
    private String scopeType;

    @Column(name = "scope_entity_id")
    private UUID scopeEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_filter")
    private Map<String, Object> scopeFilter;

    @Column(name = "protocol_id")
    private String protocolId;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "approved_by_identity_id")
    private UUID approvedByIdentityId;

    @Column(name = "phi_access_level", nullable = false)
    private String phiAccessLevel;

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private OffsetDateTime grantedAt = OffsetDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_by_identity_id")
    private UUID revokedByIdentityId;

    @Column(name = "revocation_reason")
    private String revocationReason;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "created_by_identity_id")
    private UUID createdByIdentityId;

    @Column(name = "updated_by_identity_id")
    private UUID updatedByIdentityId;

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
