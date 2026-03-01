package com.okapi.auth.repository;

import com.okapi.auth.model.db.IdentityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityRepository extends JpaRepository<IdentityEntity, UUID> {
    Optional<IdentityEntity> findByEmail(String email);

    Optional<IdentityEntity> findByExternalSubject(String externalSubject);

    Optional<IdentityEntity> findByProviderIdAndExternalSubject(String providerId, String externalSubject);

    Optional<IdentityEntity> findByUsername(String username);

    @Query("SELECT i FROM IdentityEntity i WHERE " +
            "LOWER(i.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(i.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(i.displayName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<IdentityEntity> searchByTerm(@Param("search") String search, Pageable pageable);

    Page<IdentityEntity> findByIsActive(boolean isActive, Pageable pageable);
}
