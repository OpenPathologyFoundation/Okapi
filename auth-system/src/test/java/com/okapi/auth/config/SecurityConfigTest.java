package com.okapi.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_AndRequestingProtectedResource_ThenRedirectToLogin() throws Exception {
        log.info("Testing access to protected resource /some-random-endpoint without authentication...");
        mockMvc.perform(get("/some-random-endpoint"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/okta"));
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
