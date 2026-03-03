package com.okapi.auth.repository;

import com.okapi.auth.model.db.EduCaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EduCaseRepository extends JpaRepository<EduCaseEntity, UUID> {

    Optional<EduCaseEntity> findByCaseId(String caseId);

    Page<EduCaseEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query(value = "SELECT COUNT(*) FROM wsi_edu.slides s " +
            "JOIN wsi_edu.blocks b ON s.block_id = b.id " +
            "JOIN wsi_edu.parts p ON b.part_id = p.id " +
            "WHERE p.case_id = :caseId", nativeQuery = true)
    int countSlidesByCaseId(@Param("caseId") UUID caseId);
}
