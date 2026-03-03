package com.okapi.auth.service;

import com.okapi.auth.dto.EduDtos.*;
import com.okapi.auth.model.db.EduCaseCuratorEntity;
import com.okapi.auth.model.db.EduCaseEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.EduCaseCuratorRepository;
import com.okapi.auth.repository.EduCaseRepository;
import com.okapi.auth.repository.IdentityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EduCaseService {

    private final EduCaseRepository eduCaseRepository;
    private final EduCaseCuratorRepository curatorRepository;
    private final IdentityRepository identityRepository;
    private final JdbcTemplate jdbcTemplate;

    public EduCaseService(
            EduCaseRepository eduCaseRepository,
            EduCaseCuratorRepository curatorRepository,
            IdentityRepository identityRepository,
            JdbcTemplate jdbcTemplate) {
        this.eduCaseRepository = eduCaseRepository;
        this.curatorRepository = curatorRepository;
        this.identityRepository = identityRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public EduPageResponse<EduCaseListItem> listCases(EduSearchRequest request) {
        int pageSize = request.effectivePageSize();
        int offset = request.effectivePage() * pageSize;

        // Build dynamic WHERE clause
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (request.status() != null && !request.status().isBlank()) {
            where.append(" AND c.status = ?");
            params.add(request.status());
        }

        if (request.anatomicSite() != null && !request.anatomicSite().isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM wsi_edu.parts p2 WHERE p2.case_id = c.id AND p2.anatomic_site = ?)");
            params.add(request.anatomicSite());
        }

        if (request.specimenType() != null && !request.specimenType().isBlank()) {
            where.append(" AND c.specimen_type ILIKE ?");
            params.add("%" + request.specimenType() + "%");
        }

        if (request.difficulty() != null && !request.difficulty().isBlank()) {
            where.append(" AND c.metadata->>'difficulty_level' = ?");
            params.add(request.difficulty());
        }

        if (request.stain() != null && !request.stain().isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM wsi_edu.slides s " +
                    "JOIN wsi_edu.blocks b ON s.block_id = b.id " +
                    "JOIN wsi_edu.parts p3 ON b.part_id = p3.id " +
                    "WHERE p3.case_id = c.id AND s.stain = ?)");
            params.add(request.stain());
        }

        if (request.curatorId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM wsi_edu.case_curators cc WHERE cc.case_id = c.id AND cc.identity_id = ?::uuid)");
            params.add(request.curatorId().toString());
        }

        if (request.query() != null && !request.query().isBlank()) {
            where.append(" AND (c.case_id ILIKE ? OR c.clinical_history ILIKE ? OR " +
                    "EXISTS (SELECT 1 FROM wsi_edu.parts p4 WHERE p4.case_id = c.id AND " +
                    "(p4.final_diagnosis ILIKE ? OR p4.anatomic_site ILIKE ?)))");
            String searchTerm = "%" + request.query() + "%";
            params.add(searchTerm);
            params.add(searchTerm);
            params.add(searchTerm);
            params.add(searchTerm);
        }

        // Count query
        String countSql = "SELECT COUNT(*) FROM wsi_edu.cases c " + where;
        Long totalItems = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (totalItems == null) totalItems = 0L;

        // Sort
        String orderBy = " ORDER BY c.ingested_at DESC";
        if ("caseId".equals(request.sortBy())) {
            orderBy = " ORDER BY c.case_id " + (request.sortDir() != null && "asc".equalsIgnoreCase(request.sortDir()) ? "ASC" : "DESC");
        } else if ("status".equals(request.sortBy())) {
            orderBy = " ORDER BY c.status " + (request.sortDir() != null && "asc".equalsIgnoreCase(request.sortDir()) ? "ASC" : "DESC");
        }

        // Main query
        String sql = """
                SELECT c.id, c.case_id, c.status, c.specimen_type, c.metadata,
                       (SELECT p.anatomic_site FROM wsi_edu.parts p WHERE p.case_id = c.id LIMIT 1) AS anatomic_site,
                       (SELECT p.final_diagnosis FROM wsi_edu.parts p WHERE p.case_id = c.id LIMIT 1) AS primary_diagnosis,
                       (SELECT COUNT(*) FROM wsi_edu.slides s
                        JOIN wsi_edu.blocks b ON s.block_id = b.id
                        JOIN wsi_edu.parts p ON b.part_id = p.id
                        WHERE p.case_id = c.id) AS slide_count
                FROM wsi_edu.cases c
                """ + where + orderBy + " LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        List<EduCaseListItem> items = rows.stream().map(row -> {
            UUID caseUuid = (UUID) row.get("id");
            Map<String, Object> metadata = parseJsonb(row.get("metadata"));

            // Look up primary curator display
            String primaryCuratorDisplay = curatorRepository.findByCaseIdAndRole(caseUuid, "PRIMARY_CURATOR")
                    .map(cc -> identityRepository.findById(cc.getIdentityId())
                            .map(IdentityEntity::getDisplayName)
                            .orElse(null))
                    .orElse(null);

            return new EduCaseListItem(
                    caseUuid,
                    (String) row.get("case_id"),
                    (String) row.get("status"),
                    (String) row.get("specimen_type"),
                    (String) row.get("anatomic_site"),
                    (String) row.get("primary_diagnosis"),
                    primaryCuratorDisplay,
                    ((Number) row.get("slide_count")).intValue(),
                    metadata != null ? (String) metadata.get("teaching_category") : null,
                    metadata != null ? (String) metadata.get("difficulty_level") : null,
                    metadata != null ? (List<String>) metadata.get("curriculum_tags") : null);
        }).toList();

        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        // Facets
        EduFacetCounts facets = computeFacets();

        return new EduPageResponse<>(items, request.effectivePage(), pageSize, totalItems, totalPages, facets);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public EduCaseDetail getCaseDetail(String accession) {
        EduCaseEntity caseEntity = eduCaseRepository.findByCaseId(accession)
                .orElse(null);
        if (caseEntity == null) return null;

        UUID caseId = caseEntity.getId();
        Map<String, Object> metadata = caseEntity.getMetadata();

        // Build part -> block -> slide hierarchy
        List<Map<String, Object>> partRows = jdbcTemplate.queryForList(
                "SELECT id, part_label, part_designator, anatomic_site, final_diagnosis, provenance FROM wsi_edu.parts WHERE case_id = ?::uuid ORDER BY part_label",
                caseId.toString());

        List<EduPartDetail> parts = partRows.stream().map(partRow -> {
            UUID partId = (UUID) partRow.get("id");

            List<Map<String, Object>> blockRows = jdbcTemplate.queryForList(
                    "SELECT id, block_label, block_description, provenance FROM wsi_edu.blocks WHERE part_id = ?::uuid ORDER BY block_label",
                    partId.toString());

            List<EduBlockDetail> blocks = blockRows.stream().map(blockRow -> {
                UUID blockId = (UUID) blockRow.get("id");

                List<Map<String, Object>> slideRows = jdbcTemplate.queryForList(
                        "SELECT id, slide_id, relative_path, format, stain, level_label FROM wsi_edu.slides WHERE block_id = ?::uuid ORDER BY slide_id",
                        blockId.toString());

                List<EduSlideDetail> slides = slideRows.stream().map(slideRow ->
                        new EduSlideDetail(
                                (UUID) slideRow.get("id"),
                                (String) slideRow.get("slide_id"),
                                (String) slideRow.get("relative_path"),
                                (String) slideRow.get("format"),
                                (String) slideRow.get("stain"),
                                (String) slideRow.get("level_label"))
                ).toList();

                return new EduBlockDetail(
                        blockId,
                        (String) blockRow.get("block_label"),
                        (String) blockRow.get("block_description"),
                        (String) blockRow.get("provenance"),
                        slides);
            }).toList();

            return new EduPartDetail(
                    partId,
                    (String) partRow.get("part_label"),
                    (String) partRow.get("part_designator"),
                    (String) partRow.get("anatomic_site"),
                    (String) partRow.get("final_diagnosis"),
                    (String) partRow.get("provenance"),
                    blocks);
        }).toList();

        // ICD codes
        List<Map<String, Object>> icdRows = jdbcTemplate.queryForList(
                "SELECT icd_code, code_system, code_description FROM wsi_edu.case_icd_codes WHERE case_id = ?::uuid",
                caseId.toString());
        List<EduIcdCode> icdCodes = icdRows.stream().map(row ->
                new EduIcdCode(
                        (String) row.get("icd_code"),
                        (String) row.get("code_system"),
                        (String) row.get("code_description"))
        ).toList();

        // Curators
        List<EduCaseCuratorEntity> curatorEntities = curatorRepository.findByCaseId(caseId);
        List<EduCuratorResponse> curators = curatorEntities.stream().map(cc -> {
            String display = identityRepository.findById(cc.getIdentityId())
                    .map(IdentityEntity::getDisplayName)
                    .orElse(null);
            return new EduCuratorResponse(
                    cc.getId(), cc.getCaseId(), caseEntity.getCaseId(),
                    cc.getIdentityId(), display, cc.getRole(), cc.getAssignedAt());
        }).toList();

        int slideCount = eduCaseRepository.countSlidesByCaseId(caseId);

        // Derive list-item fields from hierarchy
        String anatomicSite = parts.isEmpty() ? null : parts.getFirst().anatomicSite();
        String primaryDiagnosis = parts.isEmpty() ? null : parts.getFirst().finalDiagnosis();
        String primaryCuratorDisplay = curatorRepository.findByCaseIdAndRole(caseId, "PRIMARY_CURATOR")
                .map(cc -> identityRepository.findById(cc.getIdentityId())
                        .map(IdentityEntity::getDisplayName)
                        .orElse(null))
                .orElse(null);

        return new EduCaseDetail(
                caseId,
                caseEntity.getCaseId(),
                caseEntity.getStatus(),
                caseEntity.getSpecimenType(),
                anatomicSite,
                primaryDiagnosis,
                primaryCuratorDisplay,
                caseEntity.getClinicalHistory(),
                caseEntity.getSourceLineage(),
                metadata,
                metadata != null ? (String) metadata.get("teaching_category") : null,
                metadata != null ? (String) metadata.get("difficulty_level") : null,
                metadata != null ? (List<String>) metadata.get("curriculum_tags") : null,
                parts,
                icdCodes,
                curators,
                slideCount);
    }

    @Transactional(readOnly = true)
    public List<EduCuratorResponse> getCurators(String accession) {
        EduCaseEntity caseEntity = eduCaseRepository.findByCaseId(accession).orElse(null);
        if (caseEntity == null) return List.of();

        return curatorRepository.findByCaseId(caseEntity.getId()).stream().map(cc -> {
            String display = identityRepository.findById(cc.getIdentityId())
                    .map(IdentityEntity::getDisplayName)
                    .orElse(null);
            return new EduCuratorResponse(
                    cc.getId(), cc.getCaseId(), caseEntity.getCaseId(),
                    cc.getIdentityId(), display, cc.getRole(), cc.getAssignedAt());
        }).toList();
    }

    private EduFacetCounts computeFacets() {
        Map<String, Long> byAnatomicSite = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                "SELECT p.anatomic_site, COUNT(DISTINCT p.case_id) AS cnt FROM wsi_edu.parts p WHERE p.anatomic_site IS NOT NULL GROUP BY p.anatomic_site ORDER BY p.anatomic_site"
        ).forEach(row -> byAnatomicSite.put((String) row.get("anatomic_site"), (Long) row.get("cnt")));

        Map<String, Long> bySpecimenType = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                "SELECT c.specimen_type, COUNT(*) AS cnt FROM wsi_edu.cases c WHERE c.specimen_type IS NOT NULL GROUP BY c.specimen_type ORDER BY c.specimen_type"
        ).forEach(row -> bySpecimenType.put((String) row.get("specimen_type"), (Long) row.get("cnt")));

        Map<String, Long> byDifficulty = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                "SELECT c.metadata->>'difficulty_level' AS difficulty, COUNT(*) AS cnt FROM wsi_edu.cases c WHERE c.metadata->>'difficulty_level' IS NOT NULL GROUP BY difficulty ORDER BY difficulty"
        ).forEach(row -> byDifficulty.put((String) row.get("difficulty"), (Long) row.get("cnt")));

        Map<String, Long> byStain = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                "SELECT s.stain, COUNT(DISTINCT p.case_id) AS cnt FROM wsi_edu.slides s " +
                        "JOIN wsi_edu.blocks b ON s.block_id = b.id " +
                        "JOIN wsi_edu.parts p ON b.part_id = p.id " +
                        "WHERE s.stain IS NOT NULL GROUP BY s.stain ORDER BY s.stain"
        ).forEach(row -> byStain.put((String) row.get("stain"), (Long) row.get("cnt")));

        return new EduFacetCounts(byAnatomicSite, bySpecimenType, byDifficulty, byStain);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonb(Object jsonbValue) {
        if (jsonbValue instanceof Map) {
            return (Map<String, Object>) jsonbValue;
        }
        if (jsonbValue instanceof String str) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(str, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        }
        return Map.of();
    }
}
