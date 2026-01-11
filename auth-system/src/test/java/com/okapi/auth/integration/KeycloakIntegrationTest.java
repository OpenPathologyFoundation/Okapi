package com.okapi.auth.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @Testcontainers -- Removed
@AutoConfigureMockMvc
@Tag("integration")
@Slf4j
public class KeycloakIntegrationTest {

        // Assuming Keycloak is running locally via `docker compose up` and mapped to
        // 8180
        // Note: The docker-compose.yml maps 8180:8080
        private static final String KEYCLOAK_URL = "http://localhost:8180";

        private static final boolean KEYCLOAK_AVAILABLE = isKeycloakReachable();

        private static boolean isKeycloakReachable() {
                try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("localhost", 8180), 250);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
                if (KEYCLOAK_AVAILABLE) {
                        log.info("Configuring Keycloak OIDC issuer to: {}/realms/okapi", KEYCLOAK_URL);
                        registry.add("spring.security.oauth2.client.provider.okta.issuer-uri",
                                        () -> KEYCLOAK_URL + "/realms/okapi");
                } else {
                        // Avoid OIDC discovery at context startup when Keycloak isn't running.
                        registry.add("spring.security.oauth2.client.provider.okta.authorization-uri",
                                        () -> "http://localhost:8080/test/authorize");
                        registry.add("spring.security.oauth2.client.provider.okta.token-uri",
                                        () -> "http://localhost:8080/test/token");
                        registry.add("spring.security.oauth2.client.provider.okta.user-info-uri",
                                        () -> "http://localhost:8080/test/userinfo");
                        registry.add("spring.security.oauth2.client.provider.okta.jwk-set-uri",
                                        () -> "http://localhost:8080/test/jwks");
                }

                registry.add("spring.security.oauth2.client.registration.okta.client-id", () -> "okapi-client");
                registry.add("spring.security.oauth2.client.registration.okta.client-secret", () -> "okapi-secret");
                registry.add("spring.security.oauth2.client.registration.okta.authorization-grant-type",
                                () -> "authorization_code");
        }

        @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private com.okapi.auth.repository.IdentityRepository identityRepository;

    @Test
    void whenKeycloakIsRunning_ApplicationStartsAndConnects() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(KEYCLOAK_AVAILABLE,
                "Keycloak is not reachable on localhost:8180; skipping integration test.");

        log.info("Starting test: whenKeycloakIsRunning_ApplicationStartsAndConnects");
        log.debug("Attempting to access protected endpoint /login to verify OIDC configuration...");

        // If the context loads, we have successfully connected to the OIDC provider (Keycloak)
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
                
        // In a real full E2E, we would simulate the callback.
        // For this test, we are verifying that the app starts up, connects to Keycloak, 
        // and the persistence layer is active (repository is injectable).
        log.info("Verifying persistence layer connectivity...");
        long count = identityRepository.count();
        log.info("Current identity count in DB: {}", count);
        
        // Note: We can't easily verify the *creation* of a user without simulating 
        // the full OAuth2 authorization code flow callback which is complex to mock 
        // without a full browser driver or specialized testing lib.
        // But verifying that identityRepository.count() execution doesn't throw ensures DB is up.
        
        log.info("Successfully connected to Keycloak, accessed /login, and queried DB.");
    }
}
