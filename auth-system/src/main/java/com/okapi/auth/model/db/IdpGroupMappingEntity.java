package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(schema = "iam", name = "idp_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdpGroupMappingEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "idp_group_id")
    private UUID idpGroupId;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "description")
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            schema = "iam",
            name = "idp_group_role",
            joinColumns = @JoinColumn(name = "idp_group_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();
}
