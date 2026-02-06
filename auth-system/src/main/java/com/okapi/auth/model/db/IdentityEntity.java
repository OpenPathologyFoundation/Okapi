package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        schema = "iam",
        name = "identity",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "provider_id", "external_subject" })
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "identity_id")
    private UUID identityId;

    @Column(name = "external_subject", nullable = false)
    private String externalSubject;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "username")
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "display_short")
    private String displayShort;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "prefix")
    private String prefix;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "email")
    private String email;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false)
    @Builder.Default
    private Map<String, Object> attributes = java.util.Collections.emptyMap();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "created_by_identity_id")
    private UUID createdByIdentityId;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "updated_by_identity_id")
    private UUID updatedByIdentityId;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            schema = "iam",
            name = "identity_role",
            joinColumns = @JoinColumn(name = "identity_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @SQLJoinTableRestriction("effective_to IS NULL OR effective_to > now()")
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
