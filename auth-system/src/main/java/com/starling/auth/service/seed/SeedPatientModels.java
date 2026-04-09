package com.starling.auth.service.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * DTOs for parsing Xenonym patient seed files (e.g., xenonym-azure-vale-9728.json).
 *
 * Keep these models tolerant to forward-compatible additions in the seed file.
 */
public final class SeedPatientModels {
    private SeedPatientModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedPatientsFile(
            String seed,
            int count,
            List<SeedPatient> patients
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedPatient(
            String mrn,
            SeedPatientName name,
            String dob,
            String sex,
            SeedAddress address,
            String phone,
            String email,
            List<String> flags
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedPatientName(
            String given,
            String family,
            String display
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedAddress(
            String line,
            String city,
            String state,
            String zip,
            String country
    ) {
    }
}
