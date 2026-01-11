package com.okapi.auth.controller;

import com.okapi.auth.service.seed.IdentitySeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentitySeedService identitySeedService;

    @Test
    void seedIdentities_ShouldForbid_WhenAuthenticatedButNotAdmin() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_PATHOLOGIST")));

        mockMvc.perform(post("/admin/seed/identities").with(authentication(auth)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void seedIdentities_ShouldAllow_WhenAdmin() throws Exception {
        org.mockito.Mockito.when(identitySeedService.seedFromFile())
                .thenReturn(new IdentitySeedService.SeedRunResult(0, 0, 0, 0, 0, List.of()));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(post("/admin/seed/identities").with(authentication(auth)).with(csrf()))
                .andExpect(status().isOk());
    }
}
