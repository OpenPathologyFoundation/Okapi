package com.okapi.auth.repository;

import com.okapi.auth.model.db.EduCaseCuratorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EduCaseCuratorRepository extends JpaRepository<EduCaseCuratorEntity, UUID> {

    List<EduCaseCuratorEntity> findByCaseId(UUID caseId);

    List<EduCaseCuratorEntity> findByIdentityId(UUID identityId);

    Optional<EduCaseCuratorEntity> findByCaseIdAndRole(UUID caseId, String role);

    Optional<EduCaseCuratorEntity> findByCaseIdAndIdentityId(UUID caseId, UUID identityId);

    boolean existsByCaseIdAndIdentityId(UUID caseId, UUID identityId);
}
