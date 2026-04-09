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

@Service
public class EduCaseSeedService {

    private static final Logger log = LoggerFactory.getLogger(EduCaseSeedService.class);

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AuditEventRepository auditEventRepository;
    private final Path seedFilePath;

    public EduCaseSeedService(
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            AuditEventRepository auditEventRepository,
            @Value("${starling.seed.edu-cases.path:../seed/wsi-edu/wsi-edu-cases.v1.json}") String seedFilePath) {
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
                log.error("Failed to seed edu case {}: {}", caseId, e.getMessage(), e);
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
                .eventType("ADMIN_SEED_EDU_CASES")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed educational WSI cases run")
                .metadata(metadata)
                .build());

        return new SeedRunResult(cases.size(), created, updated, failed, results);
    }

    @SuppressWarnings("unchecked")
    private boolean upsertCase(Map<String, Object> seedCase) throws Exception {
        String caseId = (String) seedCase.get("caseId");
        String specimenType = (String) seedCase.get("specimenType");
        String clinicalHistory = (String) seedCase.get("clinicalHistory");
        String status = (String) seedCase.getOrDefault("status", "active");

        // Source lineage and metadata as JSONB strings
        Map<String, Object> sourceLineage = (Map<String, Object>) seedCase.get("sourceLineage");
        Map<String, Object> metadata = (Map<String, Object>) seedCase.get("metadata");
        String sourceLineageJson = sourceLineage != null ? objectMapper.writeValueAsString(sourceLineage) : "{}";
        String metadataJson = metadata != null ? objectMapper.writeValueAsString(metadata) : "{}";

        // Check if case already exists
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM wsi_edu.cases WHERE case_id = ?", caseId);
        boolean existed = !existing.isEmpty();

        UUID caseUuid;
        if (existed) {
            jdbcTemplate.update("""
                UPDATE wsi_edu.cases SET
                    specimen_type = ?,
                    clinical_history = ?,
                    status = ?,
                    source_lineage = ?::jsonb,
                    metadata = ?::jsonb
                WHERE case_id = ?
                """,
                    specimenType, clinicalHistory, status,
                    sourceLineageJson, metadataJson, caseId);
            caseUuid = (UUID) existing.getFirst().get("id");
        } else {
            caseUuid = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO wsi_edu.cases (id, case_id, collection, specimen_type, clinical_history,
                    status, source_lineage, metadata)
                VALUES (?::uuid, ?, 'educational', ?, ?, ?, ?::jsonb, ?::jsonb)
                """,
                    caseUuid.toString(), caseId, specimenType, clinicalHistory,
                    status, sourceLineageJson, metadataJson);
        }

        // Seed slides (parts -> blocks -> slides)
        List<Map<String, Object>> slides = (List<Map<String, Object>>) seedCase.getOrDefault("slides", List.of());
        seedSlides(caseUuid, slides);

        // Seed ICD codes
        List<Map<String, Object>> icdCodes = (List<Map<String, Object>>) seedCase.getOrDefault("icdCodes", List.of());
        seedIcdCodes(caseUuid, icdCodes);

        return existed;
    }

    private void seedSlides(UUID caseUuid, List<Map<String, Object>> slides) {
        for (Map<String, Object> slide : slides) {
            String slideId = (String) slide.get("slideId");
            String partLabel = (String) slide.get("partLabel");
            String partDesignator = (String) slide.get("partDesignator");
            String provenance = (String) slide.getOrDefault("provenance", "IMPLIED");
            String anatomicSite = (String) slide.get("anatomicSite");
            String finalDiagnosis = (String) slide.get("finalDiagnosis");
            String blockLabel = (String) slide.get("blockLabel");
            String blockDescription = (String) slide.get("blockDescription");
            String stain = (String) slide.getOrDefault("stain", "H&E");
            String format = (String) slide.getOrDefault("format", "svs");
            String relativePath = (String) slide.get("relativePath");

            if (slideId == null || relativePath == null) continue;

            // Ensure part exists
            UUID partId = ensurePart(caseUuid, partLabel, partDesignator, provenance, anatomicSite, finalDiagnosis);

            // Ensure block exists
            UUID blockId = ensureBlock(partId, blockLabel, blockDescription, provenance);

            // Ensure slide exists
            List<Map<String, Object>> existingSlide = jdbcTemplate.queryForList(
                    "SELECT id FROM wsi_edu.slides WHERE slide_id = ?", slideId);
            if (existingSlide.isEmpty()) {
                jdbcTemplate.update("""
                    INSERT INTO wsi_edu.slides (id, block_id, slide_id, relative_path, format, stain)
                    VALUES (?::uuid, ?::uuid, ?, ?, ?, ?)
                    """,
                        UUID.randomUUID().toString(), blockId.toString(),
                        slideId, relativePath, format, stain);
            }
        }
    }

    private UUID ensurePart(UUID caseUuid, String partLabel, String partDesignator,
                            String provenance, String anatomicSite, String finalDiagnosis) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM wsi_edu.parts WHERE case_id = ?::uuid AND part_label = ?",
                caseUuid.toString(), partLabel);
        if (!existing.isEmpty()) {
            return (UUID) existing.getFirst().get("id");
        }
        UUID partId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO wsi_edu.parts (id, case_id, part_label, part_designator, provenance, anatomic_site, final_diagnosis, metadata)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, '{}'::jsonb)
            """,
                partId.toString(), caseUuid.toString(), partLabel, partDesignator,
                provenance, anatomicSite, finalDiagnosis);
        return partId;
    }

    private UUID ensureBlock(UUID partId, String blockLabel, String blockDescription, String provenance) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM wsi_edu.blocks WHERE part_id = ?::uuid AND block_label = ?",
                partId.toString(), blockLabel);
        if (!existing.isEmpty()) {
            return (UUID) existing.getFirst().get("id");
        }
        UUID blockId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO wsi_edu.blocks (id, part_id, block_label, block_description, provenance)
            VALUES (?::uuid, ?::uuid, ?, ?, ?)
            """,
                blockId.toString(), partId.toString(), blockLabel, blockDescription, provenance);
        return blockId;
    }

    private void seedIcdCodes(UUID caseUuid, List<Map<String, Object>> icdCodes) {
        for (Map<String, Object> icd : icdCodes) {
            String icdCode = (String) icd.get("icd_code");
            String codeSystem = (String) icd.get("code_system");
            String codeDescription = (String) icd.get("code_description");

            if (icdCode == null || codeSystem == null) continue;

            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT 1 FROM wsi_edu.case_icd_codes WHERE case_id = ?::uuid AND icd_code = ? AND code_system = ?",
                    caseUuid.toString(), icdCode, codeSystem);
            if (existing.isEmpty()) {
                jdbcTemplate.update("""
                    INSERT INTO wsi_edu.case_icd_codes (case_id, icd_code, code_system, code_description)
                    VALUES (?::uuid, ?, ?, ?)
                    """,
                        caseUuid.toString(), icdCode, codeSystem, codeDescription);
            }
        }
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
