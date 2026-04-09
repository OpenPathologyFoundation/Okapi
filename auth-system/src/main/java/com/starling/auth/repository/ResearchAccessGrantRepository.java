package com.starling.auth.repository;

import com.starling.auth.model.db.ResearchAccessGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResearchAccessGrantRepository extends JpaRepository<ResearchAccessGrantEntity, UUID> {
    List<ResearchAccessGrantEntity> findByIdentityId(UUID identityId);

    List<ResearchAccessGrantEntity> findByRevokedAtIsNullAndExpiresAtAfter(OffsetDateTime now);
}
