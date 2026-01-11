package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "idp_group_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdpGroupMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "idp_group_name", nullable = false)
    private String idpGroupName;
}
