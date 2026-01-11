package com.okapi.auth.integration;

import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class KeycloakOidcIntegrationTest {

    private static final Path REALM_JSON = resolveRealmJson();

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("okapi_auth")
            .withUsername("okapi_service")
            .withPassword("postgres_dev_password");

    @Container
    static final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:latest")
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCopyFileToContainer(MountableFile.forHostPath(REALM_JSON), "/opt/keycloak/data/import/realm.json")
            .withCommand("start-dev", "--import-realm");

    private static Path resolveRealmJson() {
        // When run from the auth-system module, working dir is typically `.../auth-system`.
        // But IDE/CI may run from repo root.
        Path modulePath = Path.of("keycloak-data", "realm.json");
        if (Files.exists(modulePath)) {
            return modulePath.toAbsolutePath();
        }

        Path repoPath = Path.of("auth-system", "keycloak-data", "realm.json");
        if (Files.exists(repoPath)) {
            return repoPath.toAbsolutePath();
        }

        throw new IllegalStateException("Cannot find Keycloak realm file. Looked for " + modulePath + " and " + repoPath);
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // For integration tests we WANT to test OIDC discovery against a real Keycloak.
        String issuer = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/realms/okapi";
        registry.add("spring.security.oauth2.client.provider.okta.issuer-uri", () -> issuer);
        registry.add("okapi.oidc.provider-id", () -> issuer);

        registry.add("spring.security.oauth2.client.registration.okta.client-id", () -> "okapi-client");
        registry.add("spring.security.oauth2.client.registration.okta.client-secret", () -> "okapi-secret");
        registry.add("spring.security.oauth2.client.registration.okta.scope", () -> "openid,profile,email,groups");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private IdentityRepository identityRepository;

    @Test
    void oauth2AuthorizationEndpoint_redirectsToKeycloak_andDbIsMigratedAndSeeded() throws Exception {
        // 1) Verify OIDC wiring: hitting the authorization endpoint should redirect to Keycloak.
        String expectedAuthBase = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080)
                + "/realms/okapi/protocol/openid-connect/auth";

        mockMvc.perform(get("/oauth2/authorization/okta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(expectedAuthBase)));

        // 2) Verify Flyway ran (schema exists) and seed data is present.
        assertThat(roleRepository.findByName("ADMIN")).isPresent();
        assertThat(roleRepository.findByName("PATHOLOGIST")).isPresent();

        // 3) Basic repository query should work (ensures JPA mappings are compatible with the migrated schema).
        identityRepository.count();
    }
}
