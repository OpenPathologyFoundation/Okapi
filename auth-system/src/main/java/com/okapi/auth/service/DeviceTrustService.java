package com.okapi.auth.service;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.model.db.SessionDeviceEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.SessionDeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeviceTrustService {

    private final IdentityRepository identityRepository;
    private final SessionDeviceRepository sessionDeviceRepository;
    private final AuthAuditService authAuditService;
    private final long trustTtlDays;

    public DeviceTrustService(
            IdentityRepository identityRepository,
            SessionDeviceRepository sessionDeviceRepository,
            AuthAuditService authAuditService,
            @Value("${okapi.device.trust.ttl-days:30}") long trustTtlDays) {
        this.identityRepository = identityRepository;
        this.sessionDeviceRepository = sessionDeviceRepository;
        this.authAuditService = authAuditService;
        this.trustTtlDays = trustTtlDays;
    }

    public List<SessionDeviceEntity> listDevices(Identity identity) {
        UUID identityId = resolveIdentityId(identity);
        return sessionDeviceRepository.findByIdentityIdAndRevokedAtIsNullOrderByLastSeenAtDesc(identityId);
    }

    public SessionDeviceEntity trustDevice(Identity identity, String fingerprint, boolean alreadyHashed,
            Map<String, Object> metadata) {
        UUID identityId = resolveIdentityId(identity);
        String fingerprintHash = alreadyHashed ? fingerprint : sha256Hex(fingerprint);

        SessionDeviceEntity device = sessionDeviceRepository
                .findByIdentityIdAndDeviceFingerprintHashAndRevokedAtIsNull(identityId, fingerprintHash)
                .orElseGet(() -> SessionDeviceEntity.builder()
                        .identityId(identityId)
                        .deviceFingerprintHash(fingerprintHash)
                        .build());

        OffsetDateTime now = OffsetDateTime.now();
        device.setLastSeenAt(now);
        device.setTrustedUntil(now.plusDays(trustTtlDays));
        if (metadata != null && !metadata.isEmpty()) {
            device.setMetadata(metadata);
        }

        SessionDeviceEntity saved = sessionDeviceRepository.save(device);
        authAuditService.recordDeviceTrusted(identity, saved.getDeviceId());
        return saved;
    }

    public void revokeDevice(Identity identity, UUID deviceId) {
        UUID identityId = resolveIdentityId(identity);
        SessionDeviceEntity device = sessionDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        if (!identityId.equals(device.getIdentityId())) {
            throw new IllegalStateException("Device does not belong to identity");
        }
        device.setRevokedAt(OffsetDateTime.now());
        device.setRevokedByIdentityId(identityId);
        sessionDeviceRepository.save(device);
        authAuditService.recordDeviceRevoked(identity, deviceId);
    }

    public int revokeAll(Identity identity) {
        UUID identityId = resolveIdentityId(identity);
        List<SessionDeviceEntity> devices = sessionDeviceRepository
                .findByIdentityIdAndRevokedAtIsNullOrderByLastSeenAtDesc(identityId);
        OffsetDateTime now = OffsetDateTime.now();
        for (SessionDeviceEntity device : devices) {
            device.setRevokedAt(now);
            device.setRevokedByIdentityId(identityId);
            sessionDeviceRepository.save(device);
        }
        if (!devices.isEmpty()) {
            authAuditService.recordDeviceRevoked(identity, null);
        }
        return devices.size();
    }

    private UUID resolveIdentityId(Identity identity) {
        IdentityEntity entity = identityRepository
                .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                .orElseThrow(() -> new IllegalStateException("Identity not found"));
        return entity.getIdentityId();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash fingerprint", e);
        }
    }
}
