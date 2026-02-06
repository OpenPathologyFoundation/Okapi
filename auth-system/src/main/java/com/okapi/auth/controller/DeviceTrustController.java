package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.SessionDeviceEntity;
import com.okapi.auth.service.DeviceTrustService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/devices")
public class DeviceTrustController {

    private final DeviceTrustService deviceTrustService;

    public DeviceTrustController(DeviceTrustService deviceTrustService) {
        this.deviceTrustService = deviceTrustService;
    }

    public record DeviceTrustRequest(String fingerprint, Boolean hashed, Map<String, Object> metadata) {
    }

    public record DeviceResponse(
            UUID deviceId,
            OffsetDateTime firstSeenAt,
            OffsetDateTime lastSeenAt,
            OffsetDateTime trustedUntil,
            OffsetDateTime revokedAt,
            Map<String, Object> metadata) {
        static DeviceResponse from(SessionDeviceEntity entity) {
            return new DeviceResponse(
                    entity.getDeviceId(),
                    entity.getFirstSeenAt(),
                    entity.getLastSeenAt(),
                    entity.getTrustedUntil(),
                    entity.getRevokedAt(),
                    entity.getMetadata());
        }
    }

    @GetMapping
    public List<DeviceResponse> listDevices(@AuthenticationPrincipal Object principal) {
        Identity identity = requireIdentity(principal);
        return deviceTrustService.listDevices(identity).stream()
                .map(DeviceResponse::from)
                .toList();
    }

    @PostMapping
    public DeviceResponse trustDevice(
            @AuthenticationPrincipal Object principal,
            @RequestBody DeviceTrustRequest request) {
        Identity identity = requireIdentity(principal);
        String fingerprint = request == null ? null : request.fingerprint();
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint is required");
        }
        boolean hashed = request != null && Boolean.TRUE.equals(request.hashed());
        Map<String, Object> metadata = request == null ? Map.of() : request.metadata();
        return DeviceResponse.from(deviceTrustService.trustDevice(identity, fingerprint, hashed, metadata));
    }

    @DeleteMapping("/{deviceId}")
    public Map<String, Object> revokeDevice(
            @AuthenticationPrincipal Object principal,
            @PathVariable UUID deviceId) {
        Identity identity = requireIdentity(principal);
        deviceTrustService.revokeDevice(identity, deviceId);
        return Map.of("revoked", true, "deviceId", deviceId);
    }

    @PostMapping("/revoke-all")
    public Map<String, Object> revokeAll(@AuthenticationPrincipal Object principal) {
        Identity identity = requireIdentity(principal);
        int count = deviceTrustService.revokeAll(identity);
        return Map.of("revoked", count);
    }

    private Identity requireIdentity(Object principal) {
        if (principal instanceof Identity identity) {
            return identity;
        }
        throw new IllegalStateException("unauthenticated");
    }
}
