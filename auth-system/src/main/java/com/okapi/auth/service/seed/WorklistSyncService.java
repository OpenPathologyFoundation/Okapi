package com.okapi.auth.service.seed;

import com.okapi.auth.model.db.*;
import com.okapi.auth.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Populates the pathology_worklist read model from wsi.cases + JOINs.
 * This replaces mock/synthetic data with real database-driven cases.
 */
@Service
public class WorklistSyncService {

    private static final Logger log = LoggerFactory.getLogger(WorklistSyncService.class);

    private final CaseRepository caseRepository;
    private final PatientRepository patientRepository;
    private final CasePathologistRepository casePathologistRepository;
    private final IdentityRepository identityRepository;
    private final WorklistRepository worklistRepository;
    private final AuditEventRepository auditEventRepository;

    public WorklistSyncService(
            CaseRepository caseRepository,
            PatientRepository patientRepository,
            CasePathologistRepository casePathologistRepository,
            IdentityRepository identityRepository,
            WorklistRepository worklistRepository,
            AuditEventRepository auditEventRepository) {
        this.caseRepository = caseRepository;
        this.patientRepository = patientRepository;
        this.casePathologistRepository = casePathologistRepository;
        this.identityRepository = identityRepository;
        this.worklistRepository = worklistRepository;
        this.auditEventRepository = auditEventRepository;
    }

    public record SyncResult(
            int total,
            int created,
            int updated,
            int failed,
            List<SyncCaseResult> results) {
    }

    public record SyncCaseResult(
            String caseId,
            String status,
            String message) {

        public static SyncCaseResult ok(String caseId, String status) {
            return new SyncCaseResult(caseId, status, null);
        }

        public static SyncCaseResult failed(String caseId, String message) {
            return new SyncCaseResult(caseId, "FAILED", message);
        }
    }

    @Transactional
    public SyncResult syncFromCases() {
        long startedAt = System.currentTimeMillis();
        int created = 0;
        int updated = 0;
        int failed = 0;
        List<SyncCaseResult> results = new ArrayList<>();

        List<CaseEntity> cases = caseRepository.findAll();

        for (CaseEntity wsiCase : cases) {
            try {
                String accessionNumber = wsiCase.getCaseId();

                WorklistItemEntity entity = worklistRepository
                        .findByAccessionNumber(accessionNumber)
                        .orElseGet(() -> WorklistItemEntity.builder().build());

                boolean isNew = entity.getId() == null;

                // Core identification
                entity.setAccessionNumber(accessionNumber);
                entity.setCaseUuid(wsiCase.getId());

                // Patient info
                if (wsiCase.getPatientId() != null) {
                    patientRepository.findById(wsiCase.getPatientId()).ifPresent(patient -> {
                        entity.setPatientMrn(patient.getMrn());
                        entity.setPatientDisplay(patient.getDisplayName());
                    });
                }

                // Service — real service line would come from LIS; default to SURGICAL
                entity.setService("SURGICAL");

                // Specimen
                entity.setSpecimenType(wsiCase.getSpecimenType());

                // Status — wsi.cases uses lowercase (e.g. "pending_review"), worklist uses uppercase
                entity.setStatus(mapStatus(wsiCase.getStatus()));

                // Priority
                entity.setPriority(mapPriority(wsiCase.getPriority()));

                // Slide count
                int slideCount = caseRepository.countSlidesByCaseId(wsiCase.getId());
                entity.setSlideCount(slideCount);
                entity.setSlideScanned(slideCount);
                entity.setSlidePending(0);

                // Assignment — look up PRIMARY pathologist
                casePathologistRepository.findByCaseIdAndDesignation(wsiCase.getId(), "PRIMARY")
                        .ifPresentOrElse(
                                assignment -> {
                                    entity.setAssignedToIdentityId(assignment.getIdentityId());
                                    identityRepository.findById(assignment.getIdentityId())
                                            .ifPresent(identity ->
                                                    entity.setAssignedToDisplay(identity.getDisplayName()));
                                },
                                () -> {
                                    entity.setAssignedToIdentityId(null);
                                    entity.setAssignedToDisplay(null);
                                });

                // Timestamps
                entity.setCaseDate(wsiCase.getAccessionDate() != null
                        ? wsiCase.getAccessionDate()
                        : LocalDate.now());
                entity.setReceivedAt(OffsetDateTime.now());

                // Enrichment defaults
                if (entity.getAnnotations() == null) entity.setAnnotations(List.of());
                if (entity.getAlerts() == null) entity.setAlerts(List.of());
                if (entity.getMetadata() == null) entity.setMetadata(Map.of());

                worklistRepository.save(entity);

                if (isNew) {
                    created++;
                    results.add(SyncCaseResult.ok(accessionNumber, "CREATED"));
                } else {
                    updated++;
                    results.add(SyncCaseResult.ok(accessionNumber, "UPDATED"));
                }
            } catch (Exception e) {
                failed++;
                log.error("Failed to sync case {}: {}", wsiCase.getCaseId(), e.getMessage(), e);
                results.add(SyncCaseResult.failed(wsiCase.getCaseId(), e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> metadata = Map.of(
                "total", cases.size(),
                "created", created,
                "updated", updated,
                "failed", failed,
                "duration_ms", durationMs);

        auditEventRepository.save(AuditEventEntity.builder()
                .eventType("ADMIN_SEED_WORKLIST_SYNC")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Worklist sync from wsi.cases")
                .metadata(metadata)
                .build());

        return new SyncResult(cases.size(), created, updated, failed, results);
    }

    private String mapStatus(String wsiStatus) {
        if (wsiStatus == null) return "ACCESSIONED";
        return switch (wsiStatus.toLowerCase()) {
            case "pending_review" -> "PENDING_SIGNOUT";
            case "under_review" -> "UNDER_REVIEW";
            case "signed_out" -> "SIGNED_OUT";
            case "amended" -> "AMENDED";
            case "grossing" -> "GROSSING";
            case "processing" -> "PROCESSING";
            case "slides_cut" -> "SLIDES_CUT";
            case "accessioned" -> "ACCESSIONED";
            default -> wsiStatus.toUpperCase();
        };
    }

    private String mapPriority(String wsiPriority) {
        if (wsiPriority == null) return "ROUTINE";
        return switch (wsiPriority.toLowerCase()) {
            case "stat" -> "STAT";
            case "urgent" -> "URGENT";
            case "routine" -> "ROUTINE";
            default -> wsiPriority.toUpperCase();
        };
    }
}
