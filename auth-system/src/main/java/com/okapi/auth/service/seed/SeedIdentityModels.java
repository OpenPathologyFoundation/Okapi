package com.okapi.auth.service.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTOs for parsing `seed/identities/demo-identities.v1.json`.
 *
 * Keep these models tolerant to forward-compatible additions in the seed file.
 */
public final class SeedIdentityModels {
    private SeedIdentityModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedIdentitiesFile(
            String version,
            String providerId,
            List<SeedIdentity> identities
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedIdentity(
            String subject,
            String provider_id,
            String username,
            String email,
            String display_name,
            SeedName name,
            SeedDisplay display,
            List<String> idp_groups,
            Map<String, Object> local
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedName(
            String given_name,
            String family_name,
            String middle_name,
            String middle_initial,
            String nickname,
            String prefix,
            String suffix
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedDisplay(
            String full,
            @JsonProperty("short") String shortName
    ) {
    }
}
