package com.starling.auth.repository;

import com.starling.auth.model.db.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID> {

    Optional<CaseEntity> findByCaseId(String caseId);

    @Query(value = "SELECT COUNT(*) FROM wsi.slides s " +
            "JOIN wsi.blocks b ON s.block_id = b.id " +
            "JOIN wsi.parts p ON b.part_id = p.id " +
            "WHERE p.case_id = :caseId", nativeQuery = true)
    int countSlidesByCaseId(@Param("caseId") UUID caseId);
}
