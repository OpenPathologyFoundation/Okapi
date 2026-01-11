package com.okapi.auth.repository;

import com.okapi.auth.model.db.IdpGroupMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdpGroupMappingRepository extends JpaRepository<IdpGroupMappingEntity, Long> {
    Optional<IdpGroupMappingEntity> findByIdpGroupName(String idpGroupName);

    List<IdpGroupMappingEntity> findByIdpGroupNameIn(List<String> idpGroupNames);

    @Query(value = """
            select distinct r.name
            from idp_group_mappings m
            join idp_group_role_mappings gr on gr.idp_group_mapping_id = m.id
            join roles r on r.id = gr.role_id
            where m.provider_id = :providerId
              and m.idp_group_name in (:groupNames)
            """, nativeQuery = true)
    List<String> findRoleNamesByProviderIdAndGroupNames(
            @Param("providerId") String providerId,
            @Param("groupNames") List<String> groupNames);
}
