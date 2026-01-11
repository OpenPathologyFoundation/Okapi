package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(
        name = "identities",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "provider_id", "external_subject" })
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_subject", nullable = false)
    private String externalSubject;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

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

    @Column(name = "middle_initial")
    private String middleInitial;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "prefix")
    private String prefix;

    @Column(name = "suffix")
    private String suffix;

    @Column(unique = true)
    private String email;

    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private String accountType = "PRODUCTION";

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_test_user", nullable = false)
    @Builder.Default
    private boolean isTestUser = false;

    @Column(name = "is_demo_user", nullable = false)
    @Builder.Default
    private boolean isDemoUser = false;

    @Column(name = "break_glass_enabled", nullable = false)
    @Builder.Default
    private boolean breakGlassEnabled = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false)
    @Builder.Default
    private Map<String, Object> attributes = java.util.Collections.emptyMap();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "identity_roles", joinColumns = @JoinColumn(name = "identity_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();
}
