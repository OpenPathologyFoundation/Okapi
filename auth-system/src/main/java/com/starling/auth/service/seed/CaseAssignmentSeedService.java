package com.starling.auth.service.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.auth.model.db.AuditEventEntity;
import com.starling.auth.model.db.CaseEntity;
import com.starling.auth.model.db.CasePathologistEntity;
import com.starling.auth.model.db.IdentityEntity;
import com.starling.auth.repository.AuditEventRepository;
import com.starling.auth.repository.CasePathologistRepository;
import com.starling.auth.repository.CaseRepository;
import com.starling.auth.repository.IdentityRepository;
import com.starling.auth.repository.WorklistRepository;
import com.starling.auth.service.seed.SeedCaseAssignmentModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class CaseAssignmentSeedService {

    private final ObjectMapper objectMapper;
    private final CaseRepository caseRepository;
    private final CasePathologistRepository casePathologistRepository;
    private final IdentityRepository identityRepository;
    private final WorklistRepository worklistRepository;
    private final AuditEventRepository auditEventRepository;
    private final Path seedFilePath;

    public CaseAssignmentSeedService(
            ObjectMapper objectMapper,
            CaseRepository caseRepository,
            CasePathologistRepository casePathologistRepository,
            IdentityRepository identityRepository,
            WorklistRepository worklistRepository,
            AuditEventRepository auditEventRepository,
            @Value("${starling.seed.case-assignments.path:../seed/wsi/case-assignments.v1.json}") String seedFilePath) {
        this.objectMapper = objectMapper;
        this.caseRepository = caseRepository;
        this.casePathologistRepository = casePathologistRepository;
        this.identityRepository = identityRepository;
        this.worklistRepository = worklistRepository;
        this.auditEventRepository = auditEventRepository;
        this.seedFilePath = Path.of(seedFilePath);
    }

    public record SeedRunResult(
            int total,
            int created,
            int updated,
            int skipped,
            int failed,
            List<SeedAssignmentResult> results) {
    }

    public record SeedAssignmentResult(
            String accessionNumber,
            String username,
            String designation,
            String status,
            String message) {

        public static SeedAssignmentResult ok(String accession, String username, String designation, String status) {
            return new SeedAssignmentResult(accession, username, designation, status, null);
        }

        public static SeedAssignmentResult failed(String accession, String username, String designation, String message) {
            return new SeedAssignmentResult(accession, username, designation, "FAILED", message);
        }
    }

    @Transactional
    public SeedRunResult seedFromFile() {
        long startedAt = System.currentTimeMillis();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        SeedAssignmentsFile file = readSeedFile();
        List<SeedCaseAssignment> assignments = file.assignments() == null ? List.of() : file.assignments();

        List<SeedAssignmentResult> results = new ArrayList<>();
        int total = 0;

        for (SeedCaseAssignment caseAssignment : assignments) {
            String accession = caseAssignment.accessionNumber();

            if (accession == null || accession.isBlank()) {
                failed++;
                total++;
                results.add(SeedAssignmentResult.failed(accession, null, null, "Missing accession_number"));
                continue;
            }

            // Resolve case UUID from accession number
            Optional<CaseEntity> caseOpt = caseRepository.findByCaseId(accession);
            if (caseOpt.isEmpty()) {
                failed++;
                total++;
                results.add(SeedAssignmentResult.failed(accession, null, null, "Case not found: " + accession));
                continue;
            }
            UUID caseId = caseOpt.get().getId();

            List<SeedPathologistAssignment> pathologists = caseAssignment.pathologists();
            if (pathologists == null) {
                continue;
            }

            for (SeedPathologistAssignment pa : pathologists) {
                total++;
                String username = pa.username();
                String designation = pa.designation();

                try {
                    Optional<IdentityEntity> identityOpt = findIdentityByUsername(username);
                    if (identityOpt.isEmpty()) {
                        failed++;
                        results.add(SeedAssignmentResult.failed(accession, username, designation,
                                "Identity not found: " + username));
                        continue;
                    }

                    IdentityEntity identity = identityOpt.get();
                    UUID identityId = identity.getIdentityId();

                    // Upsert: check if assignment already exists
                    Optional<CasePathologistEntity> existingOpt =
                            casePathologistRepository.findByCaseIdAndIdentityId(caseId, identityId);

                    if (existingOpt.isPresent()) {
                        CasePathologistEntity existing = existingOpt.get();
                        existing.setDesignation(designation);
                        existing.setSequence(pa.sequence() != null ? pa.sequence() : 1);
                        casePathologistRepository.save(existing);
                        updated++;
                        results.add(SeedAssignmentResult.ok(accession, username, designation, "UPDATED"));
                    } else {
                        CasePathologistEntity entity = CasePathologistEntity.builder()
                                .caseId(caseId)
                                .identityId(identityId)
                                .designation(designation)
                                .sequence(pa.sequence() != null ? pa.sequence() : 1)
                                .build();
                        casePathologistRepository.save(entity);
                        created++;
                        results.add(SeedAssignmentResult.ok(accession, username, designation, "CREATED"));
                    }

                    // Sync worklist if PRIMARY
                    if ("PRIMARY".equals(designation)) {
                        worklistRepository.findByAccessionNumber(accession)
                                .ifPresent(worklist -> {
                                    worklist.setAssignedToIdentityId(identityId);
                                    worklist.setAssignedToDisplay(identity.getDisplayName());
                                    worklistRepository.save(worklist);
                                });
                    }
                } catch (Exception e) {
                    failed++;
                    results.add(SeedAssignmentResult.failed(accession, username, designation, e.getMessage()));
                }
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> metadata = Map.of(
                "total", total,
                "created", created,
                "updated", updated,
                "skipped", skipped,
                "failed", failed,
                "duration_ms", durationMs,
                "seed_file", seedFilePath.toString());

        auditEventRepository.save(AuditEventEntity.builder()
                .eventType("ADMIN_SEED_CASE_ASSIGNMENTS")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed case assignments run")
                .metadata(metadata)
                .build());

        return new SeedRunResult(total, created, updated, skipped, failed, results);
    }

    private SeedAssignmentsFile readSeedFile() {
        try {
            if (!Files.exists(seedFilePath)) {
                throw new IllegalStateException("Seed file not found: " + seedFilePath.toAbsolutePath());
            }
            return objectMapper.readValue(seedFilePath.toFile(), SeedAssignmentsFile.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read seed file: " + seedFilePath + ": " + e.getMessage(), e);
        }
    }

    private Optional<IdentityEntity> findIdentityByUsername(String username) {
        Optional<IdentityEntity> byUsername = identityRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return identityRepository.findAll().stream()
                .filter(identity -> {
                    if (identity.getAttributes() == null) return false;
                    Object seedUsername = identity.getAttributes().get("seed_username");
                    return username.equals(seedUsername);
                })
                .findFirst();
    }
}
