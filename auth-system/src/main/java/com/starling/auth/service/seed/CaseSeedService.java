package com.starling.auth.service.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.auth.model.db.AuditEventEntity;
import com.starling.auth.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Seeds wsi.cases (with parts, blocks, slides) from the JSON seed file.
 * Uses JdbcTemplate for native SQL since there are no JPA entities for
 * parts/blocks/slides.
 */
@Service
public class CaseSeedService {

    private static final Logger log = LoggerFactory.getLogger(CaseSeedService.class);

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AuditEventRepository auditEventRepository;
    private final Path seedFilePath;

    public CaseSeedService(
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            AuditEventRepository auditEventRepository,
            @Value("${starling.seed.wsi-cases.path:../seed/wsi/wsi-test-cases.v1.json}") String seedFilePath) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.auditEventRepository = auditEventRepository;
        this.seedFilePath = Path.of(seedFilePath);
    }

    public record SeedRunResult(
            int total,
            int created,
            int updated,
            int failed,
            List<SeedCaseResult> results) {
    }

    public record SeedCaseResult(
            String caseId,
            String status,
            String message) {

        public static SeedCaseResult ok(String caseId, String status) {
            return new SeedCaseResult(caseId, status, null);
        }

        public static SeedCaseResult failed(String caseId, String message) {
            return new SeedCaseResult(caseId, "FAILED", message);
        }
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public SeedRunResult seedFromFile() {
        long startedAt = System.currentTimeMillis();
        int created = 0;
        int updated = 0;
        int failed = 0;
        List<SeedCaseResult> results = new ArrayList<>();

        Map<String, Object> file = readSeedFile();
        List<Map<String, Object>> cases = (List<Map<String, Object>>) file.getOrDefault("cases", List.of());

        for (Map<String, Object> seedCase : cases) {
            String caseId = (String) seedCase.get("caseId");
            if (caseId == null || caseId.isBlank()) {
                failed++;
                results.add(SeedCaseResult.failed(null, "Missing caseId"));
                continue;
            }

            try {
                boolean existed = upsertCase(seedCase);
                if (existed) {
                    updated++;
                    results.add(SeedCaseResult.ok(caseId, "UPDATED"));
                } else {
                    created++;
                    results.add(SeedCaseResult.ok(caseId, "CREATED"));
                }
            } catch (Exception e) {
                failed++;
                log.error("Failed to seed case {}: {}", caseId, e.getMessage(), e);
                results.add(SeedCaseResult.failed(caseId, e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> metadata = Map.of(
                "total", cases.size(),
                "created", created,
                "updated", updated,
                "failed", failed,
                "duration_ms", durationMs,
                "seed_file", seedFilePath.toString());

        auditEventRepository.save(AuditEventEntity.builder()
                .eventType("ADMIN_SEED_CASES")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed WSI cases run")
                .metadata(metadata)
                .build());

        return new SeedRunResult(cases.size(), created, updated, failed, results);
    }

    @SuppressWarnings("unchecked")
    private boolean upsertCase(Map<String, Object> seedCase) {
        String caseId = (String) seedCase.get("caseId");
        String specimenType = (String) seedCase.get("specimenType");
        String clinicalHistory = (String) seedCase.get("clinicalHistory");
        String accessionDate = (String) seedCase.get("accessionDate");
        String status = (String) seedCase.getOrDefault("status", "pending_review");
        String priority = (String) seedCase.getOrDefault("priority", "routine");

        // Check if case already exists
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM wsi.cases WHERE collection = 'clinical' AND case_id = ?", caseId);
        boolean existed = !existing.isEmpty();

        // Read patient MRN from seed data (data-driven, no hardcoded mapping)
        String patientMrn = (String) seedCase.get("patientMrn");

        UUID caseUuid;
        if (existed) {
            // Update existing
            jdbcTemplate.update("""
                UPDATE wsi.cases SET
                    specimen_type = ?,
                    clinical_history = ?,
                    accession_date = ?::date,
                    status = ?,
                    priority = ?,
                    patient_id = (SELECT id FROM core.patients WHERE mrn = ?),
                    metadata = '{}'::jsonb
                WHERE collection = 'clinical' AND case_id = ?
                """,
                    specimenType, clinicalHistory, accessionDate,
                    status, priority, patientMrn, caseId);
            caseUuid = (UUID) existing.getFirst().get("id");
        } else {
            // Insert new
            caseUuid = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO wsi.cases (id, case_id, collection, specimen_type, clinical_history,
                    accession_date, status, priority, patient_id, metadata)
                VALUES (?::uuid, ?, 'clinical', ?, ?, ?::date, ?, ?,
                    (SELECT id FROM core.patients WHERE mrn = ?), '{}'::jsonb)
                """,
                    caseUuid.toString(), caseId, specimenType, clinicalHistory,
                    accessionDate, status, priority, patientMrn);
        }

        // Seed slides (parts -> blocks -> slides)
        List<Map<String, Object>> slides = (List<Map<String, Object>>) seedCase.getOrDefault("slides", List.of());
        seedSlides(caseUuid, caseId, slides);

        return existed;
    }

    private void seedSlides(UUID caseUuid, String caseId, List<Map<String, Object>> slides) {
        // Group slides by part description to create part/block/slide hierarchy
        // Slide IDs follow pattern: {caseId}_{Part}{Block}_S{N}
        // e.g. S26-0001_A1_S1 -> Part A, Block 1, Slide S1
        for (Map<String, Object> slide : slides) {
            String slideId = (String) slide.get("slideId");
            String partDesc = (String) slide.get("partDescription");
            String blockDesc = (String) slide.get("blockDescription");
            String stain = (String) slide.getOrDefault("stain", "H&E");
            String filename = (String) slide.get("filename");

            if (slideId == null || filename == null) continue;

            // Parse part label and block label from slideId
            // Pattern: {caseId}_{PartLabel}{BlockLabel}_S{N} or {caseId}-{part}-{block}-{slide}
            String suffix = slideId.substring(caseId.length());
            String partLabel;
            String blockLabel;
            String levelLabel;

            if (suffix.startsWith("_")) {
                // Format: _A1_S1
                suffix = suffix.substring(1); // "A1_S1"
                partLabel = suffix.substring(0, 1); // "A"
                String rest = suffix.substring(1); // "1_S1"
                int underscoreIdx = rest.indexOf('_');
                blockLabel = rest.substring(0, underscoreIdx); // "1"
                levelLabel = rest.substring(underscoreIdx + 1); // "S1"
            } else if (suffix.startsWith("-")) {
                // Format: -01-01-01 (dash-separated parts)
                String[] parts = suffix.substring(1).split("-");
                partLabel = parts[0]; // "01"
                blockLabel = parts[1]; // "01"
                levelLabel = "S" + parts[2]; // "S01"
            } else {
                continue;
            }

            // Ensure part exists
            UUID partId = ensurePart(caseUuid, partLabel, partDesc);

            // Ensure block exists
            UUID blockId = ensureBlock(partId, blockLabel, blockDesc);

            // Ensure slide exists
            String format = filename.endsWith(".ome.tiff") ? "ome.tiff"
                    : filename.substring(filename.lastIndexOf('.') + 1);
            String relativePath = caseId.substring(caseId.length() - 4, caseId.length() - 3).equals("6")
                    ? "2026/" + caseId + "/" + filename
                    : caseId.substring(0, 3) + "/" + caseId + "/" + filename;
            // Simpler: always use year prefix from accession
            relativePath = "2026/" + caseId + "/" + filename;

            List<Map<String, Object>> existingSlide = jdbcTemplate.queryForList(
                    "SELECT id FROM wsi.slides WHERE slide_id = ?", slideId);
            if (existingSlide.isEmpty()) {
                jdbcTemplate.update("""
                    INSERT INTO wsi.slides (id, block_id, slide_id, relative_path, format, stain, level_label)
                    VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?)
                    """,
                        UUID.randomUUID().toString(), blockId.toString(),
                        slideId, relativePath, format, stain, levelLabel);
            }
        }
    }

    private UUID ensurePart(UUID caseUuid, String partLabel, String partDesc) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM wsi.parts WHERE case_id = ?::uuid AND part_label = ?",
                caseUuid.toString(), partLabel);
        if (!existing.isEmpty()) {
            return (UUID) existing.getFirst().get("id");
        }
        UUID partId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO wsi.parts (id, case_id, part_label, part_designator, metadata)
            VALUES (?::uuid, ?::uuid, ?, ?, '{}'::jsonb)
            """,
                partId.toString(), caseUuid.toString(), partLabel, partDesc);
        return partId;
    }

    private UUID ensureBlock(UUID partId, String blockLabel, String blockDesc) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM wsi.blocks WHERE part_id = ?::uuid AND block_label = ?",
                partId.toString(), blockLabel);
        if (!existing.isEmpty()) {
            return (UUID) existing.getFirst().get("id");
        }
        UUID blockId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO wsi.blocks (id, part_id, block_label, block_description)
            VALUES (?::uuid, ?::uuid, ?, ?)
            """,
                blockId.toString(), partId.toString(), blockLabel, blockDesc);
        return blockId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSeedFile() {
        try {
            if (!Files.exists(seedFilePath)) {
                throw new IllegalStateException("Seed file not found: " + seedFilePath.toAbsolutePath());
            }
            return objectMapper.readValue(seedFilePath.toFile(), Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read seed file: " + seedFilePath + ": " + e.getMessage(), e);
        }
    }
}
