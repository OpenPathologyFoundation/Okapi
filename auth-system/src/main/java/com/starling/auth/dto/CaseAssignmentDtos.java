package com.starling.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CaseAssignmentDtos {

    private CaseAssignmentDtos() {
    }

    public static final List<String> VALID_DESIGNATIONS = List.of(
            "PRIMARY", "SECONDARY", "CONSULTING", "GROSSING");

    // ── Request Records ─────────────────────────────────────────────

    public record CaseAssignmentRequest(
            UUID identityId,
            String designation,
            Integer sequence) {
    }

    public record ReassignPrimaryRequest(
            UUID identityId) {
    }

    // ── Response Records ────────────────────────────────────────────

    public record CaseAssignmentResponse(
            UUID id,
            UUID caseId,
            String caseAccession,
            UUID identityId,
            String identityDisplay,
            String designation,
            Integer sequence,
            Instant assignedAt,
            UUID assignedBy,
            String assignedByDisplay) {

        public static CaseAssignmentResponse from(
                com.starling.auth.model.db.CasePathologistEntity entity,
                String caseAccession,
                String identityDisplay,
                String assignedByDisplay) {
            return new CaseAssignmentResponse(
                    entity.getId(),
                    entity.getCaseId(),
                    caseAccession,
                    entity.getIdentityId(),
                    identityDisplay,
                    entity.getDesignation(),
                    entity.getSequence(),
                    entity.getAssignedAt(),
                    entity.getAssignedBy(),
                    assignedByDisplay);
        }
    }
}
