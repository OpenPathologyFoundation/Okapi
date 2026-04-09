package com.starling.auth.service.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.auth.model.db.AuditEventEntity;
import com.starling.auth.model.db.PatientEntity;
import com.starling.auth.repository.AuditEventRepository;
import com.starling.auth.repository.PatientRepository;
import com.starling.auth.service.seed.SeedPatientModels.SeedPatientsFile;
import com.starling.auth.service.seed.SeedPatientModels.SeedPatient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatientSeedService {

    private final ObjectMapper objectMapper;
    private final PatientRepository patientRepository;
    private final AuditEventRepository auditEventRepository;
    private final Path seedFilePath;

    public PatientSeedService(
            ObjectMapper objectMapper,
            PatientRepository patientRepository,
            AuditEventRepository auditEventRepository,
            @Value("${starling.seed.patients.path:../seed/patients/xenonym-azure-vale-9728.json}") String seedFilePath
    ) {
        this.objectMapper = objectMapper;
        this.patientRepository = patientRepository;
        this.auditEventRepository = auditEventRepository;
        this.seedFilePath = Path.of(seedFilePath);
    }

    public record SeedRunResult(
            int total,
            int created,
            int updated,
            int skipped,
            int failed,
            List<SeedPatientResult> results
    ) {
    }

    public record SeedPatientResult(
            String mrn,
            String displayName,
            String status,
            String message
    ) {
        public static SeedPatientResult ok(String mrn, String displayName, String status) {
            return new SeedPatientResult(mrn, displayName, status, null);
        }

        public static SeedPatientResult failed(String mrn, String displayName, String message) {
            return new SeedPatientResult(mrn, displayName, "FAILED", message);
        }
    }

    @Transactional
    public SeedRunResult seedFromFile() {
        long startedAt = System.currentTimeMillis();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        SeedPatientsFile file = readSeedFile();
        List<SeedPatient> patients = file.patients() == null ? List.of() : file.patients();

        java.util.ArrayList<SeedPatientResult> results = new java.util.ArrayList<>();

        for (SeedPatient seed : patients) {
            String mrn = seed.mrn();
            String displayName = seed.name() != null ? seed.name().display() : null;

            if (mrn == null || mrn.isBlank()) {
                failed++;
                results.add(SeedPatientResult.failed(mrn, displayName, "Missing required field: mrn"));
                continue;
            }
            if (seed.name() == null || seed.name().given() == null || seed.name().family() == null) {
                failed++;
                results.add(SeedPatientResult.failed(mrn, displayName, "Missing required fields: name.given/name.family"));
                continue;
            }

            try {
                PatientEntity entity = patientRepository
                        .findByMrn(mrn)
                        .orElseGet(() -> PatientEntity.builder().build());

                boolean isNew = entity.getId() == null;

                entity.setMrn(mrn);
                entity.setGivenName(seed.name().given());
                entity.setFamilyName(seed.name().family());
                entity.setDisplayName(seed.name().display() != null ? seed.name().display()
                        : seed.name().given() + " " + seed.name().family());

                if (seed.dob() != null && !seed.dob().isBlank()) {
                    entity.setDob(LocalDate.parse(seed.dob()));
                }
                if (seed.sex() != null && !seed.sex().isBlank()) {
                    entity.setSex(seed.sex());
                }
                if (seed.phone() != null && !seed.phone().isBlank()) {
                    entity.setPhone(seed.phone());
                }
                if (seed.email() != null && !seed.email().isBlank()) {
                    entity.setEmail(seed.email());
                }

                // Address as JSONB map
                if (seed.address() != null) {
                    Map<String, Object> addr = new HashMap<>();
                    if (seed.address().line() != null) addr.put("line", seed.address().line());
                    if (seed.address().city() != null) addr.put("city", seed.address().city());
                    if (seed.address().state() != null) addr.put("state", seed.address().state());
                    if (seed.address().zip() != null) addr.put("zip", seed.address().zip());
                    if (seed.address().country() != null) addr.put("country", seed.address().country());
                    entity.setAddress(addr);
                }

                // Flags
                List<String> flags = seed.flags() != null ? seed.flags() : List.of();
                entity.setTestPatient(flags.contains("test_patient"));

                // Metadata: store non-standard flags and seed provenance
                Map<String, Object> metadata = new HashMap<>();
                if (entity.getMetadata() != null) {
                    metadata.putAll(entity.getMetadata());
                }
                List<String> edgeCases = flags.stream()
                        .filter(f -> !f.equals("test_patient"))
                        .toList();
                if (!edgeCases.isEmpty()) {
                    metadata.put("edge_cases", edgeCases);
                }
                if (file.seed() != null) {
                    metadata.put("xenonym_seed", file.seed());
                }
                entity.setMetadata(metadata);

                patientRepository.save(entity);

                if (isNew) {
                    created++;
                    results.add(SeedPatientResult.ok(mrn, displayName, "CREATED"));
                } else {
                    updated++;
                    results.add(SeedPatientResult.ok(mrn, displayName, "UPDATED"));
                }
            } catch (Exception e) {
                failed++;
                results.add(SeedPatientResult.failed(mrn, displayName, e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> auditMeta = Map.of(
                "total", patients.size(),
                "created", created,
                "updated", updated,
                "skipped", skipped,
                "failed", failed,
                "duration_ms", durationMs,
                "seed_file", seedFilePath.toString()
        );

        auditEventRepository.save(AuditEventEntity.builder()
                .eventType("ADMIN_SEED_PATIENTS")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed patients run")
                .metadata(auditMeta)
                .build());

        return new SeedRunResult(patients.size(), created, updated, skipped, failed, results);
    }

    private SeedPatientsFile readSeedFile() {
        try {
            if (!Files.exists(seedFilePath)) {
                throw new IllegalStateException("Seed file not found: " + seedFilePath.toAbsolutePath());
            }
            return objectMapper.readValue(seedFilePath.toFile(), SeedPatientsFile.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read seed file: " + seedFilePath + ": " + e.getMessage(), e);
        }
    }
}
