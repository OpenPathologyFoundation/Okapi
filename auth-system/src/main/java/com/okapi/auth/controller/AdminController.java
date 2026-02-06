package com.okapi.auth.controller;

import com.okapi.auth.dto.AdminDtos.*;
import com.okapi.auth.model.Identity;
import com.okapi.auth.service.AdminService;
import com.okapi.auth.service.SessionTimeoutService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final SessionTimeoutService sessionTimeoutService;

    public AdminController(AdminService adminService, SessionTimeoutService sessionTimeoutService) {
        this.adminService = adminService;
        this.sessionTimeoutService = sessionTimeoutService;
    }

    // ── Identities ─────────────────────────────────────────────────

    @GetMapping("/identities")
    public PageResponse<IdentitySummary> listIdentities(
            @AuthenticationPrincipal Identity actor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listIdentities(actor, search, active, page, size);
    }

    @GetMapping("/identities/{id}")
    public IdentityDetail getIdentity(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id) {
        return adminService.getIdentity(actor, id);
    }

    @PatchMapping("/identities/{id}/status")
    public ResponseEntity<Void> setIdentityStatus(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id,
            @RequestBody IdentityStatusRequest request) {
        adminService.setIdentityActive(actor, id, request.active());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/identities/{id}/roles")
    public IdentityDetail getIdentityRoles(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id) {
        return adminService.getIdentity(actor, id);
    }

    @PostMapping("/identities/{id}/roles")
    public ResponseEntity<Void> assignRole(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id,
            @RequestBody RoleAssignmentRequest request) {
        adminService.assignRole(actor, id, request.roleId(), request.justification());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/identities/{id}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id,
            @PathVariable UUID roleId) {
        adminService.revokeRole(actor, id, roleId, null);
        return ResponseEntity.noContent().build();
    }

    // ── Roles ──────────────────────────────────────────────────────

    @GetMapping("/roles")
    public List<RoleSummary> listRoles(@AuthenticationPrincipal Identity actor) {
        return adminService.listRoles(actor);
    }

    @GetMapping("/roles/permissions")
    public List<RolePermissionRow> getRolePermissionMatrix(@AuthenticationPrincipal Identity actor) {
        return adminService.getRolePermissionMatrix(actor);
    }

    // ── IdP Mappings ───────────────────────────────────────────────

    @GetMapping("/idp-mappings")
    public List<IdpMappingSummary> listIdpMappings(@AuthenticationPrincipal Identity actor) {
        return adminService.listIdpMappings(actor);
    }

    @PostMapping("/idp-mappings")
    public IdpMappingSummary createIdpMapping(
            @AuthenticationPrincipal Identity actor,
            @RequestBody IdpMappingCreateRequest request) {
        return adminService.createIdpMapping(actor, request);
    }

    @DeleteMapping("/idp-mappings/{id}")
    public ResponseEntity<Void> deleteIdpMapping(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id) {
        adminService.deleteIdpMapping(actor, id);
        return ResponseEntity.noContent().build();
    }

    // ── Audit Events ───────────────────────────────────────────────

    @GetMapping("/audit-events")
    public PageResponse<AuditEventSummary> listAuditEvents(
            @AuthenticationPrincipal Identity actor,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listAuditEvents(actor, eventType, actorId, targetType, outcome, from, to, page, size);
    }

    // ── Break-Glass Grants ─────────────────────────────────────────

    @GetMapping("/grants/break-glass")
    public List<BreakGlassGrantSummary> listActiveBreakGlassGrants(@AuthenticationPrincipal Identity actor) {
        return adminService.listActiveBreakGlassGrants(actor);
    }

    @DeleteMapping("/grants/break-glass/{id}")
    public ResponseEntity<Void> revokeBreakGlassGrant(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id) {
        adminService.revokeBreakGlassGrant(actor, id);
        return ResponseEntity.noContent().build();
    }

    // ── Research Grants ────────────────────────────────────────────

    @GetMapping("/grants/research")
    public List<ResearchGrantSummary> listActiveResearchGrants(@AuthenticationPrincipal Identity actor) {
        return adminService.listActiveResearchGrants(actor);
    }

    @DeleteMapping("/grants/research/{id}")
    public ResponseEntity<Void> revokeResearchGrant(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id,
            @RequestBody(required = false) ResearchGrantRevokeRequest request) {
        String reason = request != null ? request.reason() : null;
        adminService.revokeResearchGrant(actor, id, reason);
        return ResponseEntity.noContent().build();
    }

    // ── Devices ────────────────────────────────────────────────────

    @GetMapping("/devices")
    public List<DeviceSummary> listDevices(
            @AuthenticationPrincipal Identity actor,
            @RequestParam UUID identityId) {
        return adminService.listDevicesForIdentity(actor, identityId);
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Void> revokeDevice(
            @AuthenticationPrincipal Identity actor,
            @PathVariable UUID id) {
        adminService.revokeDevice(actor, id);
        return ResponseEntity.noContent().build();
    }

    // ── Dashboard ──────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public DashboardSummary getDashboard() {
        return adminService.getDashboardSummary();
    }

    // ── Settings ────────────────────────────────────────────────────

    @GetMapping("/settings/session-timeout")
    public Map<String, Integer> getSessionTimeout() {
        return Map.of("timeoutMinutes", sessionTimeoutService.getIdleTimeoutMinutes());
    }

    @PutMapping("/settings/session-timeout")
    public ResponseEntity<Map<String, Integer>> setSessionTimeout(
            @AuthenticationPrincipal Identity actor,
            @RequestBody Map<String, Integer> body) {
        Integer minutes = body.get("timeoutMinutes");
        if (minutes == null) {
            return ResponseEntity.badRequest().build();
        }
        sessionTimeoutService.setIdleTimeoutMinutes(actor, minutes);
        return ResponseEntity.ok(Map.of("timeoutMinutes", minutes));
    }
}
