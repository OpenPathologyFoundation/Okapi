package com.okapi.auth.service.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okapi.auth.model.db.AuditEventEntity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.AuditEventRepository;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.service.keycloak.KeycloakAdminClient;
import com.okapi.auth.service.seed.SeedIdentityModels.SeedIdentitiesFile;
import com.okapi.auth.service.seed.SeedIdentityModels.SeedIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class IdentitySeedService {

    private final ObjectMapper objectMapper;
    private final KeycloakAdminClient keycloakAdminClient;
    private final IdentityRepository identityRepository;
    private final AuditEventRepository auditEventRepository;
    private final String keycloakRealm;
    private final Path seedFilePath;

    public IdentitySeedService(
            ObjectMapper objectMapper,
            KeycloakAdminClient keycloakAdminClient,
            IdentityRepository identityRepository,
            AuditEventRepository auditEventRepository,
            @Value("${okapi.keycloak.realm:okapi}") String keycloakRealm,
            @Value("${okapi.seed.identities.path:../seed/identities/demo-identities.v1.json}") String seedFilePath
    ) {
        this.objectMapper = objectMapper;
        this.keycloakAdminClient = keycloakAdminClient;
        this.identityRepository = identityRepository;
        this.auditEventRepository = auditEventRepository;
        this.keycloakRealm = keycloakRealm;
        this.seedFilePath = Path.of(seedFilePath);
    }

    public record SeedRunResult(
            int total,
            int created,
            int updated,
            int skipped,
            int failed,
            List<SeedIdentityResult> results
    ) {
    }

    public record SeedIdentityResult(
            String username,
            String subject,
            String providerId,
            String status,
            String message
    ) {
        public static SeedIdentityResult ok(String username, String subject, String providerId, String status) {
            return new SeedIdentityResult(username, subject, providerId, status, null);
        }

        public static SeedIdentityResult failed(String username, String subject, String providerId, String message) {
            return new SeedIdentityResult(username, subject, providerId, "FAILED", message);
        }
    }

    @Transactional
    public SeedRunResult seedFromFile() {
        long startedAt = System.currentTimeMillis();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        SeedIdentitiesFile file = readSeedFile();
        List<SeedIdentity> identities = file.identities() == null ? List.of() : file.identities();

        java.util.ArrayList<SeedIdentityResult> results = new java.util.ArrayList<>();

        for (SeedIdentity seed : identities) {
            String username = seed.username();
            String subject = seed.subject();
            String providerId = firstNonBlank(seed.provider_id(), file.providerId());

            if (subject == null || subject.isBlank() || username == null || username.isBlank()) {
                failed++;
                results.add(SeedIdentityResult.failed(username, subject, providerId, "Missing required fields: subject/username"));
                continue;
            }
            if (providerId == null || providerId.isBlank()) {
                failed++;
                results.add(SeedIdentityResult.failed(username, subject, providerId, "Missing providerId (identity.provider_id or top-level providerId)"));
                continue;
            }

            try {
                // Validate existence in Keycloak (source of truth for accounts)
                KeycloakAdminClient.KeycloakUser kcUser = keycloakAdminClient.getUserById(keycloakRealm, subject);
                if (kcUser == null || kcUser.id() == null || kcUser.id().isBlank()) {
                    throw new IllegalStateException("Keycloak user not found for subject: " + subject);
                }
                if (kcUser.username() != null && !kcUser.username().isBlank() && !Objects.equals(kcUser.username(), username)) {
                    throw new IllegalStateException("Keycloak username mismatch for subject. Expected '" + username + "' but got '" + kcUser.username() + "'.");
                }

                IdentityEntity entity = identityRepository
                        .findByProviderIdAndExternalSubject(providerId, subject)
                        .orElseGet(() -> IdentityEntity.builder().build());

                boolean isNew = entity.getIdentityId() == null;

                entity.setProviderId(providerId);
                entity.setExternalSubject(subject);
                entity.setUsername(username);

                // Seed-controlled fields (clinical display accuracy)
                String displayFull = seed.display() != null && seed.display().full() != null && !seed.display().full().isBlank()
                        ? seed.display().full()
                        : (seed.display_name() == null ? null : seed.display_name());
                if (displayFull != null && !displayFull.isBlank()) {
                    entity.setDisplayName(displayFull);
                }
                if (seed.display() != null && seed.display().shortName() != null && !seed.display().shortName().isBlank()) {
                    entity.setDisplayShort(seed.display().shortName());
                }
                if (seed.name() != null) {
                    setIfNonBlank(entity::setGivenName, seed.name().given_name());
                    setIfNonBlank(entity::setFamilyName, seed.name().family_name());
                    setIfNonBlank(entity::setMiddleName, seed.name().middle_name());
                    setIfNonBlank(entity::setPrefix, seed.name().prefix());
                    setIfNonBlank(entity::setSuffix, seed.name().suffix());
                }

                // Email: use seed if present, otherwise keep existing or use Keycloak value
                if (seed.email() != null && !seed.email().isBlank()) {
                    entity.setEmail(seed.email());
                } else if ((entity.getEmail() == null || entity.getEmail().isBlank()) && kcUser.email() != null && !kcUser.email().isBlank()) {
                    entity.setEmail(kcUser.email());
                }

                // Local flags from seed (stored in attributes for now)
                Map<String, Object> local = seed.local() == null ? Map.of() : seed.local();

                // Attributes: keep seed metadata to support future automation (permission groups etc.)
                Map<String, Object> mergedAttributes = new HashMap<>();
                if (entity.getAttributes() != null) {
                    mergedAttributes.putAll(entity.getAttributes());
                }
                mergedAttributes.put("seed_username", username);
                mergedAttributes.put("seed_idp_groups", seed.idp_groups() == null ? List.of() : seed.idp_groups());
                mergedAttributes.put("seed_local", local);
                if (seed.name() != null) {
                    if (seed.name().middle_initial() != null && !seed.name().middle_initial().isBlank()) {
                        mergedAttributes.put("middle_initial", seed.name().middle_initial());
                    }
                    if (seed.name().nickname() != null && !seed.name().nickname().isBlank()) {
                        mergedAttributes.put("nickname", seed.name().nickname());
                    }
                }
                entity.setAttributes(mergedAttributes);

                identityRepository.save(entity);

                if (isNew) {
                    created++;
                    results.add(SeedIdentityResult.ok(username, subject, providerId, "CREATED"));
                } else {
                    updated++;
                    results.add(SeedIdentityResult.ok(username, subject, providerId, "UPDATED"));
                }
            } catch (Exception e) {
                failed++;
                results.add(SeedIdentityResult.failed(username, subject, providerId, e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> metadata = Map.of(
                "total", identities.size(),
                "created", created,
                "updated", updated,
                "skipped", skipped,
                "failed", failed,
                "duration_ms", durationMs,
                "seed_file", seedFilePath.toString()
        );

        auditEventRepository.save(AuditEventEntity.builder()
                .eventType("ADMIN_SEED_IDENTITIES")
                .outcome(failed == 0 ? "SUCCESS" : "PARTIAL_FAILURE")
                .details("Seed identities run")
                .metadata(metadata)
                .build());

        return new SeedRunResult(identities.size(), created, updated, skipped, failed, results);
    }

    private SeedIdentitiesFile readSeedFile() {
        try {
            if (!Files.exists(seedFilePath)) {
                throw new IllegalStateException("Seed file not found: " + seedFilePath.toAbsolutePath());
            }
            return objectMapper.readValue(seedFilePath.toFile(), SeedIdentitiesFile.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read seed file: " + seedFilePath + ": " + e.getMessage(), e);
        }
    }

    private static void setIfNonBlank(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
