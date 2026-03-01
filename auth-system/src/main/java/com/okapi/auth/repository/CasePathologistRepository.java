package com.okapi.auth.repository;

import com.okapi.auth.model.db.CasePathologistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CasePathologistRepository extends JpaRepository<CasePathologistEntity, UUID> {

    List<CasePathologistEntity> findByCaseIdOrderBySequence(UUID caseId);

    List<CasePathologistEntity> findByIdentityId(UUID identityId);

    Optional<CasePathologistEntity> findByCaseIdAndDesignation(UUID caseId, String designation);

    Optional<CasePathologistEntity> findByCaseIdAndIdentityId(UUID caseId, UUID identityId);

    boolean existsByCaseIdAndIdentityId(UUID caseId, UUID identityId);

    void deleteByCaseIdAndIdentityId(UUID caseId, UUID identityId);
}
