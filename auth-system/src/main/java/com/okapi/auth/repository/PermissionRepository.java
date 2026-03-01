package com.okapi.auth.repository;

import com.okapi.auth.model.db.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
    @Query(value = """
            select distinct p.name
            from iam.permission p
            join iam.role_permission rp on rp.permission_id = p.permission_id
            join iam.role r on r.role_id = rp.role_id
            where r.name in (:roleNames)
            """, nativeQuery = true)
    List<String> findPermissionNamesByRoleNames(@Param("roleNames") List<String> roleNames);
}
