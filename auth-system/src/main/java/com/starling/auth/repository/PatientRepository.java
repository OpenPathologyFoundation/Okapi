package com.starling.auth.repository;

import com.starling.auth.model.db.PatientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

    Optional<PatientEntity> findByMrn(String mrn);

    List<PatientEntity> findByFamilyNameAndGivenName(String familyName, String givenName);

    Page<PatientEntity> findByIsTestPatient(boolean isTestPatient, Pageable pageable);

    Page<PatientEntity> findByIsActive(boolean isActive, Pageable pageable);

    @Query("SELECT p FROM PatientEntity p WHERE " +
            "LOWER(p.mrn) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.familyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.givenName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<PatientEntity> searchByTerm(@Param("search") String search, Pageable pageable);
}
