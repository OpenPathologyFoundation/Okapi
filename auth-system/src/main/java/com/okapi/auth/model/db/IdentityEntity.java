package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "identities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_subject", unique = true, nullable = false)
    private String externalSubject;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "display_name")
    private String displayName;

    @Column(unique = true)
    private String email;

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
