package com.okapi.auth.service.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTOs for parsing {@code seed/wsi/case-assignments.v1.json}.
 */
public final class SeedCaseAssignmentModels {
    private SeedCaseAssignmentModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedAssignmentsFile(
            String version,
            List<SeedCaseAssignment> assignments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedCaseAssignment(
            @JsonProperty("accession_number") String accessionNumber,
            List<SeedPathologistAssignment> pathologists) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedPathologistAssignment(
            String username,
            String designation,
            Integer sequence) {
    }
}
