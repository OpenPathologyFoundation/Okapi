package com.okapi.auth.service;

import com.okapi.auth.dto.EduDtos.*;
import com.okapi.auth.model.db.EduNamedCollectionEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.EduNamedCollectionRepository;
import com.okapi.auth.repository.IdentityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EduCollectionService {

    private final EduNamedCollectionRepository collectionRepository;
    private final IdentityRepository identityRepository;
    private final JdbcTemplate jdbcTemplate;

    public EduCollectionService(
            EduNamedCollectionRepository collectionRepository,
            IdentityRepository identityRepository,
            JdbcTemplate jdbcTemplate) {
        this.collectionRepository = collectionRepository;
        this.identityRepository = identityRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<EduNamedCollectionResponse> listCollections(UUID userId) {
        // Show owned + DEPARTMENT + INSTITUTION visibility
        List<EduNamedCollectionEntity> collections;
        if (userId != null) {
            // Get collections the user can see
            collections = collectionRepository.findAll().stream()
                    .filter(c -> c.getOwnerId().equals(userId)
                            || "DEPARTMENT".equals(c.getVisibility())
                            || "INSTITUTION".equals(c.getVisibility()))
                    .toList();
        } else {
            collections = collectionRepository.findByVisibilityIn(List.of("DEPARTMENT", "INSTITUTION"));
        }

        return collections.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public EduNamedCollectionDetail getCollectionDetail(UUID collectionId) {
        EduNamedCollectionEntity entity = collectionRepository.findById(collectionId).orElse(null);
        if (entity == null) return null;

        String ownerDisplay = identityRepository.findById(entity.getOwnerId())
                .map(IdentityEntity::getDisplayName)
                .orElse(null);

        // Get member cases ordered by sequence
        List<Map<String, Object>> caseRows = jdbcTemplate.queryForList("""
                SELECT c.id, c.case_id, c.status, c.specimen_type, c.metadata,
                       (SELECT p.anatomic_site FROM wsi_edu.parts p WHERE p.case_id = c.id LIMIT 1) AS anatomic_site,
                       (SELECT p.final_diagnosis FROM wsi_edu.parts p WHERE p.case_id = c.id LIMIT 1) AS primary_diagnosis,
                       (SELECT COUNT(*) FROM wsi_edu.slides s
                        JOIN wsi_edu.blocks b ON s.block_id = b.id
                        JOIN wsi_edu.parts p ON b.part_id = p.id
                        WHERE p.case_id = c.id) AS slide_count
                FROM wsi_edu.cases c
                JOIN wsi_edu.collection_cases cc ON c.id = cc.case_id
                WHERE cc.collection_id = ?::uuid
                ORDER BY cc.sequence, c.case_id
                """, collectionId.toString());

        List<EduCaseListItem> cases = caseRows.stream().map(row -> {
            Map<String, Object> metadata = parseJsonb(row.get("metadata"));
            return new EduCaseListItem(
                    (UUID) row.get("id"),
                    (String) row.get("case_id"),
                    (String) row.get("status"),
                    (String) row.get("specimen_type"),
                    (String) row.get("anatomic_site"),
                    (String) row.get("primary_diagnosis"),
                    null,
                    ((Number) row.get("slide_count")).intValue(),
                    metadata != null ? (String) metadata.get("teaching_category") : null,
                    metadata != null ? (String) metadata.get("difficulty_level") : null,
                    metadata != null ? (List<String>) metadata.get("curriculum_tags") : null);
        }).toList();

        return new EduNamedCollectionDetail(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                ownerDisplay,
                entity.getVisibility(),
                cases,
                entity.getCreatedAt());
    }

    private EduNamedCollectionResponse toResponse(EduNamedCollectionEntity entity) {
        String ownerDisplay = identityRepository.findById(entity.getOwnerId())
                .map(IdentityEntity::getDisplayName)
                .orElse(null);

        int caseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wsi_edu.collection_cases WHERE collection_id = ?::uuid",
                Integer.class,
                entity.getId().toString());

        return new EduNamedCollectionResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerId(),
                ownerDisplay,
                entity.getVisibility(),
                caseCount,
                entity.getCreatedAt());
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
