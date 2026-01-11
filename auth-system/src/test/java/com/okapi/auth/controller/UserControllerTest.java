package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authMe_ShouldReturnMeResponse_WhenAuthenticated() throws Exception {
        Identity identity = Identity.builder()
                .providerId("http://localhost:8180/realms/okapi")
                .externalSubject("sub-123")
                .email("user@okapi.invalid")
                .displayName("User Example")
                .displayShort("Example, U.")
                .givenName("User")
                .familyName("Example")
                .middleName("Q")
                .middleInitial("Q")
                .prefix("Dr")
                .suffix("MD")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_PATHOLOGIST")))
                .attributes(java.util.Map.of())
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                identity,
                "N/A",
                identity.getAuthorities());

        mockMvc.perform(get("/auth/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("http://localhost:8180/realms/okapi"))
                .andExpect(jsonPath("$.externalSubject").value("sub-123"))
                .andExpect(jsonPath("$.email").value("user@okapi.invalid"))
                .andExpect(jsonPath("$.displayName").value("User Example"))
                .andExpect(jsonPath("$.displayShort").value("Example, U."))
                .andExpect(jsonPath("$.givenName").value("User"))
                .andExpect(jsonPath("$.familyName").value("Example"))
                .andExpect(jsonPath("$.middleName").value("Q"))
                .andExpect(jsonPath("$.middleInitial").value("Q"))
                .andExpect(jsonPath("$.prefix").value("Dr"))
                .andExpect(jsonPath("$.suffix").value("MD"))
                .andExpect(jsonPath("$.authorities").isArray());
    }

    @Test
    void authMe_ShouldRedirectToLogin_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void root_ShouldRedirectToAuthMe_WhenAuthenticated() throws Exception {
        Identity identity = Identity.builder()
                .providerId("http://localhost:8180/realms/okapi")
                .externalSubject("sub-123")
                .email("user@okapi.invalid")
                .displayName("User Example")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .attributes(java.util.Map.of())
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                identity,
                "N/A",
                identity.getAuthorities());

        mockMvc.perform(get("/").with(authentication(auth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/auth/me"));
    }
}
