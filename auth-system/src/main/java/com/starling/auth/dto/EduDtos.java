package com.starling.auth.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EduDtos {

    private EduDtos() {
    }

    public static final List<String> VALID_CURATOR_ROLES = List.of(
            "PRIMARY_CURATOR", "CURATOR", "CONTRIBUTOR");

    // ── Case List Item ──────────────────────────────────────────────

    public record EduCaseListItem(
            UUID id,
            String caseId,
            String status,
            String specimenType,
            String anatomicSite,
            String primaryDiagnosis,
            String primaryCuratorDisplay,
            int slideCount,
            String teachingCategory,
            String difficultyLevel,
            List<String> curriculumTags) {
    }

    // ── Case Detail ─────────────────────────────────────────────────

    public record EduCaseDetail(
            UUID id,
            String caseId,
            String status,
            String specimenType,
            String anatomicSite,
            String primaryDiagnosis,
            String primaryCuratorDisplay,
            String clinicalHistory,
            Map<String, Object> sourceLineage,
            Map<String, Object> metadata,
            String teachingCategory,
            String difficultyLevel,
            List<String> curriculumTags,
            List<EduPartDetail> parts,
            List<EduIcdCode> icdCodes,
            List<EduCuratorResponse> curators,
            int slideCount) {
    }

    public record EduPartDetail(
            UUID id,
            String partLabel,
            String partDesignator,
            String anatomicSite,
            String finalDiagnosis,
            String provenance,
            List<EduBlockDetail> blocks) {
    }

    public record EduBlockDetail(
            UUID id,
            String blockLabel,
            String blockDescription,
            String provenance,
            List<EduSlideDetail> slides) {
    }

    public record EduSlideDetail(
            UUID id,
            String slideId,
            String relativePath,
            String format,
            String stain,
            String levelLabel) {
    }

    public record EduIcdCode(
            String icdCode,
            String codeSystem,
            String codeDescription) {
    }

    // ── Curator Response ────────────────────────────────────────────

    public record EduCuratorResponse(
            UUID id,
            UUID caseId,
            String caseAccession,
            UUID identityId,
            String identityDisplay,
            String role,
            Instant assignedAt) {
    }

    // ── Named Collection ────────────────────────────────────────────

    public record EduNamedCollectionResponse(
            UUID id,
            String name,
            String description,
            UUID ownerId,
            String ownerDisplay,
            String visibility,
            int caseCount,
            OffsetDateTime createdAt) {
    }

    public record EduNamedCollectionDetail(
            UUID id,
            String name,
            String description,
            String ownerDisplay,
            String visibility,
            List<EduCaseListItem> cases,
            OffsetDateTime createdAt) {
    }

    // ── Search / Facets ─────────────────────────────────────────────

    public record EduSearchRequest(
            String query,
            String anatomicSite,
            String specimenType,
            String difficulty,
            String stain,
            UUID curatorId,
            UUID collectionId,
            String status,
            String sortBy,
            String sortDir,
            Integer page,
            Integer pageSize) {

        public int effectivePage() {
            return page != null && page >= 0 ? page : 0;
        }

        public int effectivePageSize() {
            return pageSize != null && pageSize > 0 && pageSize <= 200 ? pageSize : 50;
        }
    }

    public record EduPageResponse<T>(
            List<T> items,
            int page,
            int pageSize,
            long totalItems,
            int totalPages,
            EduFacetCounts facets) {
    }

    public record EduFacetCounts(
            Map<String, Long> byAnatomicSite,
            Map<String, Long> bySpecimenType,
            Map<String, Long> byDifficulty,
            Map<String, Long> byStain) {
    }
}
