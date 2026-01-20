package com.okapi.auth.dto;

import com.okapi.auth.model.db.WorklistItemEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Worklist API responses and requests.
 */
public final class WorklistDtos {

    private WorklistDtos() {
    }

    // Response records

    public record WorklistItemResponse(
            Long id,
            String accessionNumber,
            String patientMrn,
            String patientDisplay,
            String service,
            String specimenType,
            String specimenSite,
            String status,
            String lisStatus,
            String wsiStatus,
            String authoringStatus,
            String priority,
            Long assignedToId,
            String assignedToDisplay,
            Integer slideCount,
            Integer slidePending,
            Integer slideScanned,
            LocalDate caseDate,
            OffsetDateTime receivedAt,
            OffsetDateTime collectedAt,
            List<Map<String, Object>> annotations,
            List<Map<String, Object>> alerts,
            Map<String, Object> metadata) {

        public static WorklistItemResponse from(WorklistItemEntity entity) {
            return new WorklistItemResponse(
                    entity.getId(),
                    entity.getAccessionNumber(),
                    entity.getPatientMrn(),
                    entity.getPatientDisplay(),
                    entity.getService(),
                    entity.getSpecimenType(),
                    entity.getSpecimenSite(),
                    entity.getStatus(),
                    entity.getLisStatus(),
                    entity.getWsiStatus(),
                    entity.getAuthoringStatus(),
                    entity.getPriority(),
                    entity.getAssignedToId(),
                    entity.getAssignedToDisplay(),
                    entity.getSlideCount(),
                    entity.getSlidePending(),
                    entity.getSlideScanned(),
                    entity.getCaseDate(),
                    entity.getReceivedAt(),
                    entity.getCollectedAt(),
                    entity.getAnnotations(),
                    entity.getAlerts(),
                    entity.getMetadata());
        }
    }

    public record WorklistPageResponse(
            List<WorklistItemResponse> items,
            int page,
            int pageSize,
            long totalItems,
            int totalPages,
            WorklistCounts counts) {
    }

    public record WorklistCounts(
            long total,
            long myCases,
            Map<String, Long> byService,
            Map<String, Long> byStatus,
            Map<String, Long> byPriority) {
    }

    // Request/filter records

    public record WorklistFilterRequest(
            List<String> services,
            List<String> statuses,
            List<String> priorities,
            Long assignedToId,
            Boolean myCasesOnly,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            Integer page,
            Integer pageSize) {

        public static WorklistFilterRequest defaults() {
            return new WorklistFilterRequest(
                    null, null, null, null, false, null, null, null,
                    "caseDate", "desc", 0, 50);
        }

        public int effectivePage() {
            return page != null && page >= 0 ? page : 0;
        }

        public int effectivePageSize() {
            return pageSize != null && pageSize > 0 && pageSize <= 200 ? pageSize : 50;
        }

        public String effectiveSortBy() {
            return sortBy != null ? sortBy : "caseDate";
        }

        public boolean isDescending() {
            return sortDir == null || "desc".equalsIgnoreCase(sortDir);
        }
    }
}
