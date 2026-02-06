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
@Table(schema = "iam", name = "session_device")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDeviceEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "device_fingerprint_hash", nullable = false)
    private String deviceFingerprintHash;

    @Column(name = "first_seen_at", nullable = false)
    @Builder.Default
    private OffsetDateTime firstSeenAt = OffsetDateTime.now();

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "trusted_until")
    private OffsetDateTime trustedUntil;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_by_identity_id")
    private UUID revokedByIdentityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = java.util.Collections.emptyMap();

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
