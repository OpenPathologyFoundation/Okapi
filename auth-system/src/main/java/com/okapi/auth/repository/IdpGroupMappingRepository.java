package com.okapi.auth.repository;

import com.okapi.auth.model.db.IdpGroupMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdpGroupMappingRepository extends JpaRepository<IdpGroupMappingEntity, Long> {
    Optional<IdpGroupMappingEntity> findByIdpGroupName(String idpGroupName);

    List<IdpGroupMappingEntity> findByIdpGroupNameIn(List<String> idpGroupNames);
}
