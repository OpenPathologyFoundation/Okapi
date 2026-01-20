package com.okapi.auth.service.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for parsing `seed/cases/synthetic-cases.v1.json`.
 *
 * Keep these models tolerant to forward-compatible additions in the seed file.
 */
public final class SeedWorklistModels {
    private SeedWorklistModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedCasesFile(
            String version,
            List<SeedCase> cases) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedCase(
            @JsonProperty("accession_number") String accessionNumber,
            SeedPatient patient,
            String service,
            SeedSpecimen specimen,
            SeedStatus status,
            String priority,
            SeedAssignment assignment,
            SeedSlides slides,
            SeedTimestamps timestamps,
            List<Map<String, Object>> annotations,
            List<Map<String, Object>> alerts,
            Map<String, Object> metadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedPatient(
            String mrn,
            String display) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedSpecimen(
            String type,
            String site) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedStatus(
            String workflow,
            String lis,
            String wsi,
            String authoring) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedAssignment(
            String username,
            String display) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedSlides(
            Integer total,
            Integer pending,
            Integer scanned) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedTimestamps(
            @JsonProperty("case_date") String caseDate,
            @JsonProperty("received_at") String receivedAt,
            @JsonProperty("collected_at") String collectedAt) {
    }
}
