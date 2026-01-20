package com.okapi.auth.service.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okapi.auth.model.db.AuditEventEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.model.db.WorklistItemEntity;
import com.okapi.auth.repository.AuditEventRepository;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.WorklistRepository;
import com.okapi.auth.service.seed.SeedWorklistModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WorklistSeedService {

    private final ObjectMapper objectMapper;
    private final WorklistRepository worklistRepository;
    private final IdentityRepository identityRepository;
    private final AuditEventRepository auditEventRepository;
    private final Path seedFilePath;

    public WorklistSeedService(
            ObjectMapper objectMapper,
            WorklistRepository worklistRepository,
            IdentityRepository identityRepository,
            AuditEventRepository auditEventRepository,
            @Value("${okapi.seed.worklist.path:../seed/cases/synthetic-cases.v1.json}") String seedFilePath) {
        this.objectMapper = objectMapper;
        this.worklistRepository = worklistRepository;
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
            List<SeedCaseResult> results) {
    }

    public record SeedCaseResult(
            String accessionNumber,
            String status,
            String message) {

        public static SeedCaseResult ok(String accessionNumber, String status) {
            return new SeedCaseResult(accessionNumber, status, null);
        }

        public static SeedCaseResult failed(String accessionNumber, String message) {
            return new SeedCaseResult(accessionNumber, "FAILED", message);
        }
    }

    @Transactional
    public SeedRunResult seedFromFile() {
        long startedAt = System.currentTimeMillis();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        SeedCasesFile file = readSeedFile();
        List<SeedCase> cases = file.cases() == null ? List.of() : file.cases();

        List<SeedCaseResult> results = new ArrayList<>();

        for (SeedCase seedCase : cases) {
            String accessionNumber = seedCase.accessionNumber();

            if (accessionNumber == null || accessionNumber.isBlank()) {
                failed++;
                results.add(SeedCaseResult.failed(accessionNumber, "Missing required field: accession_number"));
                continue;
            }

            try {
                WorklistItemEntity entity = worklistRepository
                        .findByAccessionNumber(accessionNumber)
                        .orElseGet(() -> WorklistItemEntity.builder().build());

                boolean isNew = entity.getId() == null;

                // Core identification
                entity.setAccessionNumber(accessionNumber);
                if (seedCase.patient() != null) {
                    entity.setPatientMrn(seedCase.patient().mrn());
                    entity.setPatientDisplay(seedCase.patient().display());
                }

                // Service
                entity.setService(seedCase.service() != null ? seedCase.service() : "SURGICAL");

                // Specimen
                if (seedCase.specimen() != null) {
                    entity.setSpecimenType(seedCase.specimen().type());
                    entity.setSpecimenSite(seedCase.specimen().site());
                }

                // Status
                if (seedCase.status() != null) {
                    entity.setStatus(seedCase.status().workflow() != null ? seedCase.status().workflow() : "ACCESSIONED");
                    entity.setLisStatus(seedCase.status().lis());
                    entity.setWsiStatus(seedCase.status().wsi());
                    entity.setAuthoringStatus(seedCase.status().authoring());
                }

                // Priority
                entity.setPriority(seedCase.priority() != null ? seedCase.priority() : "ROUTINE");

                // Assignment - resolve identity by username
                if (seedCase.assignment() != null && seedCase.assignment().username() != null) {
                    String username = seedCase.assignment().username();
                    // Look up identity by seed_username in attributes
                    Optional<IdentityEntity> assignee = findIdentityByUsername(username);
                    if (assignee.isPresent()) {
                        entity.setAssignedToId(assignee.get().getId());
                        entity.setAssignedToDisplay(
                                seedCase.assignment().display() != null
                                        ? seedCase.assignment().display()
                                        : assignee.get().getDisplayName());
                    } else {
                        // Fallback: use display from seed data without ID
                        entity.setAssignedToDisplay(seedCase.assignment().display());
                    }
                }

                // Slides
                if (seedCase.slides() != null) {
                    entity.setSlideCount(seedCase.slides().total() != null ? seedCase.slides().total() : 0);
                    entity.setSlidePending(seedCase.slides().pending() != null ? seedCase.slides().pending() : 0);
                    entity.setSlideScanned(seedCase.slides().scanned() != null ? seedCase.slides().scanned() : 0);
                }

                // Timestamps
                if (seedCase.timestamps() != null) {
                    if (seedCase.timestamps().caseDate() != null) {
                        entity.setCaseDate(parseDate(seedCase.timestamps().caseDate()));
                    }
                    if (seedCase.timestamps().receivedAt() != null) {
                        entity.setReceivedAt(parseOffsetDateTime(seedCase.timestamps().receivedAt()));
                    }
                    if (seedCase.timestamps().collectedAt() != null) {
                        entity.setCollectedAt(parseOffsetDateTime(seedCase.timestamps().collectedAt()));
                    }
                }

                // Enrichment
                entity.setAnnotations(seedCase.annotations() != null ? seedCase.annotations() : List.of());
                entity.setAlerts(seedCase.alerts() != null ? seedCase.alerts() : List.of());
                entity.setMetadata(seedCase.metadata() != null ? seedCase.metadata() : Map.of());

                worklistRepository.save(entity);

                if (isNew) {
                    created++;
                    results.add(SeedCaseResult.ok(accessionNumber, "CREATED"));
                } else {
                    updated++;
                    results.add(SeedCaseResult.ok(accessionNumber, "UPDATED"));
                }
            } catch (Exception e) {
                failed++;
                results.add(SeedCaseResult.failed(accessionNumber, e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> metadata = Map.of(
                "total", cases.size(),
                "created", created,
                "updated", updated,
                "skipped", skipped,
                "failed", failed,
                "duration_ms", durationMs,
                "seed_file", seedFilePath.toString());

        auditEventRepository.save(AuditEventEntity.builder()
                .eventType("ADMIN_SEED_WORKLIST")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed worklist cases run")
                .metadata(metadata)
                .build());

        return new SeedRunResult(cases.size(), created, updated, skipped, failed, results);
    }

    private SeedCasesFile readSeedFile() {
        try {
            if (!Files.exists(seedFilePath)) {
                throw new IllegalStateException("Seed file not found: " + seedFilePath.toAbsolutePath());
            }
            return objectMapper.readValue(seedFilePath.toFile(), SeedCasesFile.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read seed file: " + seedFilePath + ": " + e.getMessage(), e);
        }
    }

    private Optional<IdentityEntity> findIdentityByUsername(String username) {
        // Find identity with matching seed_username in attributes
        // This is a simple implementation; production would use a custom query
        return identityRepository.findAll().stream()
                .filter(identity -> {
                    if (identity.getAttributes() == null) return false;
                    Object seedUsername = identity.getAttributes().get("seed_username");
                    return username.equals(seedUsername);
                })
                .findFirst();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private OffsetDateTime parseOffsetDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return OffsetDateTime.now();
        }
        try {
            return OffsetDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            // Try ISO_DATE_TIME without offset
            try {
                return OffsetDateTime.parse(dateTimeStr + "Z");
            } catch (Exception e2) {
                return OffsetDateTime.now();
            }
        }
    }
}
