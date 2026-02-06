package com.okapi.auth.repository;

import com.okapi.auth.model.db.SessionDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionDeviceRepository extends JpaRepository<SessionDeviceEntity, UUID> {
    List<SessionDeviceEntity> findByIdentityIdAndRevokedAtIsNullOrderByLastSeenAtDesc(UUID identityId);

    Optional<SessionDeviceEntity> findByIdentityIdAndDeviceFingerprintHashAndRevokedAtIsNull(
            UUID identityId,
            String deviceFingerprintHash);

    List<SessionDeviceEntity> findByIdentityId(UUID identityId);

    long countByRevokedAtIsNull();
}
