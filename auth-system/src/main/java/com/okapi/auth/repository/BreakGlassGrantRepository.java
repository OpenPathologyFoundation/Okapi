package com.okapi.auth.repository;

import com.okapi.auth.model.db.BreakGlassGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BreakGlassGrantRepository extends JpaRepository<BreakGlassGrantEntity, UUID> {
    List<BreakGlassGrantEntity> findByIdentityId(UUID identityId);

    List<BreakGlassGrantEntity> findByRevokedAtIsNullAndExpiresAtAfter(OffsetDateTime now);
}
