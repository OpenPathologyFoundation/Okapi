package com.okapi.auth.service;

import com.okapi.auth.dto.CaseAssignmentDtos.*;
import com.okapi.auth.model.db.CaseEntity;
import com.okapi.auth.model.db.CasePathologistEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.model.db.WorklistItemEntity;
import com.okapi.auth.repository.CasePathologistRepository;
import com.okapi.auth.repository.CaseRepository;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.WorklistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.okapi.auth.dto.CaseAssignmentDtos.VALID_DESIGNATIONS;

@Service
public class CaseAssignmentService {

    private static final Map<String, Integer> DESIGNATION_ORDER = Map.of(
            "PRIMARY", 0, "SECONDARY", 1, "CONSULTING", 2, "GROSSING", 3);

    private final CasePathologistRepository casePathologistRepository;
    private final CaseRepository caseRepository;
    private final IdentityRepository identityRepository;
    private final WorklistRepository worklistRepository;

    public CaseAssignmentService(
            CasePathologistRepository casePathologistRepository,
            CaseRepository caseRepository,
            IdentityRepository identityRepository,
            WorklistRepository worklistRepository) {
        this.casePathologistRepository = casePathologistRepository;
        this.caseRepository = caseRepository;
        this.identityRepository = identityRepository;
        this.worklistRepository = worklistRepository;
    }

    @Transactional
    public CaseAssignmentResponse assign(UUID caseId, CaseAssignmentRequest request, UUID actingUserId) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found: " + caseId));

        IdentityEntity identity = identityRepository.findById(request.identityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Identity not found: " + request.identityId()));

        if (!identity.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identity is not active: " + request.identityId());
        }

        String designation = request.designation();
        if (designation == null || !VALID_DESIGNATIONS.contains(designation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid designation: " + designation + ". Must be one of: " + VALID_DESIGNATIONS);
        }

        if ("PRIMARY".equals(designation)) {
            casePathologistRepository.findByCaseIdAndDesignation(caseId, "PRIMARY")
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Case already has a PRIMARY assignment. Use PUT /assignments/primary to reassign.");
                    });
        }

        if (casePathologistRepository.existsByCaseIdAndIdentityId(caseId, request.identityId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Identity is already assigned to this case.");
        }

        CasePathologistEntity entity = CasePathologistEntity.builder()
                .caseId(caseId)
                .identityId(request.identityId())
                .designation(designation)
                .sequence(request.sequence() != null ? request.sequence() : 1)
                .assignedBy(actingUserId)
                .build();

        entity = casePathologistRepository.save(entity);

        if ("PRIMARY".equals(designation)) {
            syncWorklistPrimary(caseEntity.getCaseId(), identity.getIdentityId(), identity.getDisplayName());
        }

        return toResponse(entity, caseEntity, identity);
    }

    @Transactional
    public void unassign(UUID caseId, UUID identityId, UUID actingUserId) {
        CasePathologistEntity assignment = casePathologistRepository.findByCaseIdAndIdentityId(caseId, identityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));

        boolean wasPrimary = "PRIMARY".equals(assignment.getDesignation());

        casePathologistRepository.delete(assignment);

        if (wasPrimary) {
            CaseEntity caseEntity = caseRepository.findById(caseId).orElse(null);
            if (caseEntity != null) {
                syncWorklistPrimary(caseEntity.getCaseId(), null, null);
            }
        }
    }

    @Transactional
    public CaseAssignmentResponse reassignPrimary(UUID caseId, UUID newIdentityId, UUID actingUserId) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found: " + caseId));

        IdentityEntity newIdentity = identityRepository.findById(newIdentityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Identity not found: " + newIdentityId));

        if (!newIdentity.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identity is not active: " + newIdentityId);
        }

        // Demote current PRIMARY to SECONDARY (if any)
        casePathologistRepository.findByCaseIdAndDesignation(caseId, "PRIMARY")
                .ifPresent(current -> {
                    current.setDesignation("SECONDARY");
                    casePathologistRepository.save(current);
                });

        // Check if the new identity already has a different designation on this case
        CasePathologistEntity entity = casePathologistRepository.findByCaseIdAndIdentityId(caseId, newIdentityId)
                .map(existing -> {
                    existing.setDesignation("PRIMARY");
                    existing.setAssignedBy(actingUserId);
                    return casePathologistRepository.save(existing);
                })
                .orElseGet(() -> {
                    CasePathologistEntity newAssignment = CasePathologistEntity.builder()
                            .caseId(caseId)
                            .identityId(newIdentityId)
                            .designation("PRIMARY")
                            .assignedBy(actingUserId)
                            .build();
                    return casePathologistRepository.save(newAssignment);
                });

        syncWorklistPrimary(caseEntity.getCaseId(), newIdentity.getIdentityId(), newIdentity.getDisplayName());

        return toResponse(entity, caseEntity, newIdentity);
    }

    @Transactional(readOnly = true)
    public List<CaseAssignmentResponse> getAssignments(UUID caseId) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found: " + caseId));

        List<CasePathologistEntity> assignments = casePathologistRepository.findByCaseIdOrderBySequence(caseId);

        // Sort by designation priority (PRIMARY first) then sequence
        assignments.sort(Comparator
                .comparingInt((CasePathologistEntity a) -> DESIGNATION_ORDER.getOrDefault(a.getDesignation(), 99))
                .thenComparingInt(CasePathologistEntity::getSequence));

        return assignments.stream()
                .map(a -> toResponse(a, caseEntity, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CaseAssignmentResponse> getMyCases(UUID identityId) {
        List<CasePathologistEntity> assignments = casePathologistRepository.findByIdentityId(identityId);

        return assignments.stream()
                .map(a -> {
                    CaseEntity caseEntity = caseRepository.findById(a.getCaseId()).orElse(null);
                    return toResponse(a, caseEntity, null);
                })
                .toList();
    }

    // ── Private helpers ─────────────────────────────────────────────

    private void syncWorklistPrimary(String accessionNumber, UUID identityId, String displayName) {
        worklistRepository.findByAccessionNumber(accessionNumber)
                .ifPresent(worklist -> {
                    worklist.setAssignedToIdentityId(identityId);
                    worklist.setAssignedToDisplay(displayName);
                    worklistRepository.save(worklist);
                });
    }

    private CaseAssignmentResponse toResponse(CasePathologistEntity entity, CaseEntity caseEntity, IdentityEntity identity) {
        String caseAccession = caseEntity != null ? caseEntity.getCaseId() : null;

        String identityDisplay;
        if (identity != null) {
            identityDisplay = identity.getDisplayName();
        } else {
            identityDisplay = identityRepository.findById(entity.getIdentityId())
                    .map(IdentityEntity::getDisplayName)
                    .orElse(null);
        }

        String assignedByDisplay = null;
        if (entity.getAssignedBy() != null) {
            assignedByDisplay = identityRepository.findById(entity.getAssignedBy())
                    .map(IdentityEntity::getDisplayName)
                    .orElse(null);
        }

        return CaseAssignmentResponse.from(entity, caseAccession, identityDisplay, assignedByDisplay);
    }
}
