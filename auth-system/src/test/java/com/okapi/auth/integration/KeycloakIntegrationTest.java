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

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
                log.info("Configuring Keycloak OIDC issuer to: {}/realms/okapi", KEYCLOAK_URL);
                registry.add("spring.security.oauth2.client.provider.okta.issuer-uri",
                                () -> KEYCLOAK_URL + "/realms/okapi");
                registry.add("spring.security.oauth2.client.registration.okta.client-id", () -> "okapi-client");
                registry.add("spring.security.oauth2.client.registration.okta.client-secret", () -> "okapi-secret");
                registry.add("spring.security.oauth2.client.registration.okta.authorization-grant-type",
                                () -> "authorization_code");
        }

        @Autowired
        private MockMvc mockMvc;

        @Test
        void whenKeycloakIsRunning_ApplicationStartsAndConnects() throws Exception {
                log.info("Starting test: whenKeycloakIsRunning_ApplicationStartsAndConnects");
                log.debug("Attempting to access protected endpoint /login to verify OIDC configuration...");

                // If the context loads, we have successfully connected to the OIDC provider
                // (Keycloak)
                mockMvc.perform(get("/login"))
                                .andExpect(status().isOk());

                log.info("Successfully connected to Keycloak and accessed /login.");
        }
}
