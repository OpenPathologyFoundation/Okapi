package com.okapi.auth.repository;

import com.okapi.auth.model.db.IdpGroupMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdpGroupMappingRepository extends JpaRepository<IdpGroupMappingEntity, UUID> {
    Optional<IdpGroupMappingEntity> findByGroupName(String groupName);

    List<IdpGroupMappingEntity> findByGroupNameIn(List<String> groupNames);

    @Query(value = """
            select distinct r.name
            from iam.idp_group g
            join iam.idp_group_role gr on gr.idp_group_id = g.idp_group_id
            join iam.role r on r.role_id = gr.role_id
            where g.provider_id = :providerId
              and g.group_name in (:groupNames)
            """, nativeQuery = true)
    List<String> findRoleNamesByProviderIdAndGroupNames(
            @Param("providerId") String providerId,
            @Param("groupNames") List<String> groupNames);

    @Query(value = """
            select g.idp_group_id, g.provider_id, g.group_name, g.description,
                   string_agg(r.name, ',' order by r.name) as role_names
            from iam.idp_group g
            left join iam.idp_group_role gr on gr.idp_group_id = g.idp_group_id
            left join iam.role r on r.role_id = gr.role_id
            group by g.idp_group_id, g.provider_id, g.group_name, g.description
            order by g.group_name
            """, nativeQuery = true)
    List<Object[]> findAllWithRoleNames();
}
