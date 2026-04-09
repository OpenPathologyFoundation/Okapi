package com.starling.auth.service.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.auth.model.db.AuditEventEntity;
import com.starling.auth.model.db.EduCaseCuratorEntity;
import com.starling.auth.model.db.EduCaseEntity;
import com.starling.auth.model.db.IdentityEntity;
import com.starling.auth.repository.AuditEventRepository;
import com.starling.auth.repository.EduCaseCuratorRepository;
import com.starling.auth.repository.EduCaseRepository;
import com.starling.auth.repository.IdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EduCuratorSeedService {

    private static final Logger log = LoggerFactory.getLogger(EduCuratorSeedService.class);

    private final ObjectMapper objectMapper;
    private final EduCaseRepository eduCaseRepository;
    private final EduCaseCuratorRepository curatorRepository;
    private final IdentityRepository identityRepository;
    private final AuditEventRepository auditEventRepository;
    private final Path seedFilePath;

    public EduCuratorSeedService(
            ObjectMapper objectMapper,
            EduCaseRepository eduCaseRepository,
            EduCaseCuratorRepository curatorRepository,
            IdentityRepository identityRepository,
            AuditEventRepository auditEventRepository,
            @Value("${starling.seed.edu-curators.path:../seed/wsi-edu/edu-curator-assignments.v1.json}") String seedFilePath) {
        this.objectMapper = objectMapper;
        this.eduCaseRepository = eduCaseRepository;
        this.curatorRepository = curatorRepository;
        this.identityRepository = identityRepository;
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
            String accession,
            String username,
            String role,
            String status,
            String message) {

        public static SeedAssignmentResult ok(String accession, String username, String role, String status) {
            return new SeedAssignmentResult(accession, username, role, status, null);
        }

        public static SeedAssignmentResult failed(String accession, String username, String role, String message) {
            return new SeedAssignmentResult(accession, username, role, "FAILED", message);
        }
    }

    // Seed file model records
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedFile(
            String version,
            List<SeedCaseAssignment> assignments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedCaseAssignment(
            @JsonProperty("accession_number") String accessionNumber,
            List<SeedCuratorEntry> curators) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedCuratorEntry(
            String username,
            String role) {
    }

    @Transactional
    public SeedRunResult seedFromFile() {
        long startedAt = System.currentTimeMillis();
        int total = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        List<SeedAssignmentResult> results = new ArrayList<>();

        SeedFile file = readSeedFile();
        List<SeedCaseAssignment> assignments = file.assignments() != null ? file.assignments() : List.of();

        for (SeedCaseAssignment caseAssignment : assignments) {
            String accession = caseAssignment.accessionNumber();

            // Resolve case UUID from accession
            Optional<EduCaseEntity> caseOpt = eduCaseRepository.findByCaseId(accession);
            if (caseOpt.isEmpty()) {
                failed++;
                total++;
                results.add(SeedAssignmentResult.failed(accession, null, null,
                        "Case not found: " + accession));
                continue;
            }
            java.util.UUID caseId = caseOpt.get().getId();

            List<SeedCuratorEntry> curators = caseAssignment.curators() != null ? caseAssignment.curators() : List.of();
            for (SeedCuratorEntry curator : curators) {
                total++;
                String username = curator.username();
                String role = curator.role();

                // Resolve identity by username
                Optional<IdentityEntity> identityOpt = findIdentityByUsername(username);
                if (identityOpt.isEmpty()) {
                    failed++;
                    results.add(SeedAssignmentResult.failed(accession, username, role,
                            "Identity not found for username: " + username));
                    continue;
                }

                java.util.UUID identityId = identityOpt.get().getIdentityId();

                try {
                    // Upsert curator assignment
                    Optional<EduCaseCuratorEntity> existingOpt =
                            curatorRepository.findByCaseIdAndIdentityId(caseId, identityId);

                    if (existingOpt.isPresent()) {
                        EduCaseCuratorEntity existing = existingOpt.get();
                        existing.setRole(role);
                        curatorRepository.save(existing);
                        updated++;
                        results.add(SeedAssignmentResult.ok(accession, username, role, "UPDATED"));
                    } else {
                        EduCaseCuratorEntity entity = EduCaseCuratorEntity.builder()
                                .caseId(caseId)
                                .identityId(identityId)
                                .role(role)
                                .build();
                        curatorRepository.save(entity);
                        created++;
                        results.add(SeedAssignmentResult.ok(accession, username, role, "CREATED"));
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to seed curator {} for case {}: {}", username, accession, e.getMessage(), e);
                    results.add(SeedAssignmentResult.failed(accession, username, role, e.getMessage()));
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
                .eventType("ADMIN_SEED_EDU_CURATORS")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed educational curator assignments run")
                .metadata(metadata)
                .build());

        return new SeedRunResult(total, created, updated, skipped, failed, results);
    }

    private Optional<IdentityEntity> findIdentityByUsername(String username) {
        // First try direct username lookup
        Optional<IdentityEntity> byUsername = identityRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        // Fall back to seed_username in attributes map
        return identityRepository.findAll().stream()
                .filter(identity -> {
                    if (identity.getAttributes() == null) return false;
                    Object seedUsername = identity.getAttributes().get("seed_username");
                    return username.equals(seedUsername);
                })
                .findFirst();
    }

    private SeedFile readSeedFile() {
        try {
            if (!Files.exists(seedFilePath)) {
                throw new IllegalStateException("Seed file not found: " + seedFilePath.toAbsolutePath());
            }
            return objectMapper.readValue(seedFilePath.toFile(), SeedFile.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read seed file: " + seedFilePath + ": " + e.getMessage(), e);
        }
    }
}
