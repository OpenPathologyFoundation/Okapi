package com.starling.auth.config;

import com.starling.auth.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Slf4j
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_AndRequestingProtectedResource_ThenRedirectToLogin() throws Exception {
        log.info("Testing access to protected resource /some-random-endpoint without authentication...");
        mockMvc.perform(get("/some-random-endpoint"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/okta"));
        log.debug("Redirected to login as expected.");
    }

    @Test
    void whenRequestingPublicResource_ThenAccessGranted() throws Exception {
        log.info("Testing access to public resource /login...");
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
        log.debug("Access granted as expected.");
    }
}
