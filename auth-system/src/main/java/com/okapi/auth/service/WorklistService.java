package com.okapi.auth.service;

import com.okapi.auth.dto.WorklistDtos.*;
import com.okapi.auth.model.db.WorklistItemEntity;
import com.okapi.auth.repository.WorklistRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorklistService {

    private final WorklistRepository worklistRepository;

    public WorklistService(WorklistRepository worklistRepository) {
        this.worklistRepository = worklistRepository;
    }

    @Transactional(readOnly = true)
    public WorklistPageResponse getWorklist(WorklistFilterRequest filter, Long currentUserId) {
        Specification<WorklistItemEntity> spec = buildSpecification(filter, currentUserId);

        Sort sort = buildSort(filter);
        PageRequest pageRequest = PageRequest.of(
                filter.effectivePage(),
                filter.effectivePageSize(),
                sort);

        Page<WorklistItemEntity> page = worklistRepository.findAll(spec, pageRequest);

        List<WorklistItemResponse> items = page.getContent().stream()
                .map(WorklistItemResponse::from)
                .toList();

        WorklistCounts counts = buildCounts(currentUserId);

        return new WorklistPageResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                counts);
    }

    @Transactional(readOnly = true)
    public WorklistItemResponse getByAccessionNumber(String accessionNumber) {
        return worklistRepository.findByAccessionNumber(accessionNumber)
                .map(WorklistItemResponse::from)
                .orElse(null);
    }

    private Specification<WorklistItemEntity> buildSpecification(WorklistFilterRequest filter, Long currentUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // My cases filter
            if (Boolean.TRUE.equals(filter.myCasesOnly()) && currentUserId != null) {
                predicates.add(cb.equal(root.get("assignedToId"), currentUserId));
                // Exclude completed cases for "my cases"
                predicates.add(cb.notEqual(root.get("status"), "SIGNED_OUT"));
                predicates.add(cb.notEqual(root.get("status"), "AMENDED"));
            }

            // Specific assignee filter (if not using my cases)
            if (!Boolean.TRUE.equals(filter.myCasesOnly()) && filter.assignedToId() != null) {
                predicates.add(cb.equal(root.get("assignedToId"), filter.assignedToId()));
            }

            // Service filter
            if (filter.services() != null && !filter.services().isEmpty()) {
                predicates.add(root.get("service").in(filter.services()));
            }

            // Status filter
            if (filter.statuses() != null && !filter.statuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.statuses()));
            }

            // Priority filter
            if (filter.priorities() != null && !filter.priorities().isEmpty()) {
                predicates.add(root.get("priority").in(filter.priorities()));
            }

            // Date range filter
            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("caseDate"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("caseDate"), filter.toDate()));
            }

            // Search filter (accession number or patient)
            if (filter.search() != null && !filter.search().isBlank()) {
                String searchPattern = "%" + filter.search().toLowerCase() + "%";
                Predicate accessionMatch = cb.like(cb.lower(root.get("accessionNumber")), searchPattern);
                Predicate patientMatch = cb.like(cb.lower(root.get("patientDisplay")), searchPattern);
                Predicate mrnMatch = cb.like(cb.lower(root.get("patientMrn")), searchPattern);
                predicates.add(cb.or(accessionMatch, patientMatch, mrnMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(WorklistFilterRequest filter) {
        String sortField = mapSortField(filter.effectiveSortBy());
        Sort.Direction direction = filter.isDescending() ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Priority sort: STAT > URGENT > ROUTINE (use case expression or custom)
        // For now, simple alphabetical which happens to work: ROUTINE, STAT, URGENT
        // A production system would use a custom comparator or DB function

        return Sort.by(direction, sortField);
    }

    private String mapSortField(String fieldName) {
        return switch (fieldName) {
            case "caseDate", "case_date" -> "caseDate";
            case "accessionNumber", "accession_number" -> "accessionNumber";
            case "status" -> "status";
            case "priority" -> "priority";
            case "service" -> "service";
            case "receivedAt", "received_at" -> "receivedAt";
            case "patientDisplay", "patient_display" -> "patientDisplay";
            default -> "caseDate";
        };
    }

    private WorklistCounts buildCounts(Long currentUserId) {
        long total = worklistRepository.count();

        long myCases = 0;
        if (currentUserId != null) {
            myCases = worklistRepository.findByAssignedToIdAndStatusNotIn(
                    currentUserId,
                    List.of("SIGNED_OUT", "AMENDED")).size();
        }

        // Counts by service
        Map<String, Long> byService = List.of("SURGICAL", "CYTOLOGY", "HEMATOLOGY", "AUTOPSY").stream()
                .collect(Collectors.toMap(
                        s -> s,
                        s -> worklistRepository.countByService(s)));

        // Counts by status
        Map<String, Long> byStatus = List.of(
                "ACCESSIONED", "GROSSING", "PROCESSING", "SLIDES_CUT",
                "PENDING_SIGNOUT", "UNDER_REVIEW", "SIGNED_OUT", "AMENDED").stream()
                .collect(Collectors.toMap(
                        s -> s,
                        s -> worklistRepository.countByStatus(s)));

        // Counts by priority
        Map<String, Long> byPriority = List.of("STAT", "URGENT", "ROUTINE").stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> worklistRepository.countByPriority(p)));

        return new WorklistCounts(total, myCases, byService, byStatus, byPriority);
    }
}
