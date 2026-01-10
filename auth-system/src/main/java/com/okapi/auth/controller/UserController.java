package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/me")
    public Object getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal instanceof Identity) {
            return principal;
        }
        // Fallback for debugging, if it's not our Identity object yet
        return Map.of("principal", principal);
    }
}
