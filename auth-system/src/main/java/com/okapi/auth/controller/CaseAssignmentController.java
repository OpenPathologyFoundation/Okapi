package com.okapi.auth.controller;

import com.okapi.auth.dto.CaseAssignmentDtos.*;
import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.service.CaseAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
public class CaseAssignmentController {

    private final CaseAssignmentService caseAssignmentService;
    private final IdentityRepository identityRepository;

    public CaseAssignmentController(
            CaseAssignmentService caseAssignmentService,
            IdentityRepository identityRepository) {
        this.caseAssignmentService = caseAssignmentService;
        this.identityRepository = identityRepository;
    }

    @PostMapping("/{caseId}/assignments")
    public ResponseEntity<CaseAssignmentResponse> assign(
            @PathVariable UUID caseId,
            @RequestBody CaseAssignmentRequest request,
            @AuthenticationPrincipal Object principal) {
        UUID actingUserId = resolveCurrentUserId(principal);
        CaseAssignmentResponse response = caseAssignmentService.assign(caseId, request, actingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{caseId}/assignments/{identityId}")
    public ResponseEntity<Void> unassign(
            @PathVariable UUID caseId,
            @PathVariable UUID identityId,
            @AuthenticationPrincipal Object principal) {
        UUID actingUserId = resolveCurrentUserId(principal);
        caseAssignmentService.unassign(caseId, identityId, actingUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{caseId}/assignments")
    public ResponseEntity<List<CaseAssignmentResponse>> getAssignments(
            @PathVariable UUID caseId) {
        return ResponseEntity.ok(caseAssignmentService.getAssignments(caseId));
    }

    @PutMapping("/{caseId}/assignments/primary")
    public ResponseEntity<CaseAssignmentResponse> reassignPrimary(
            @PathVariable UUID caseId,
            @RequestBody ReassignPrimaryRequest request,
            @AuthenticationPrincipal Object principal) {
        UUID actingUserId = resolveCurrentUserId(principal);
        CaseAssignmentResponse response = caseAssignmentService.reassignPrimary(caseId, request.identityId(), actingUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-cases")
    public ResponseEntity<List<CaseAssignmentResponse>> getMyCases(
            @AuthenticationPrincipal Object principal) {
        UUID currentUserId = resolveCurrentUserId(principal);
        if (currentUserId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(caseAssignmentService.getMyCases(currentUserId));
    }

    private UUID resolveCurrentUserId(Object principal) {
        if (principal instanceof Identity identity) {
            return identityRepository
                    .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                    .map(IdentityEntity::getIdentityId)
                    .orElse(null);
        }
        return null;
    }
}
