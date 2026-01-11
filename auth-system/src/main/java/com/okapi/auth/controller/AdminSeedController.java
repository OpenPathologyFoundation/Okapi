package com.okapi.auth.controller;

import com.okapi.auth.service.seed.IdentitySeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seed")
public class AdminSeedController {

    private final IdentitySeedService identitySeedService;

    public AdminSeedController(IdentitySeedService identitySeedService) {
        this.identitySeedService = identitySeedService;
    }

    @PostMapping("/identities")
    public ResponseEntity<IdentitySeedService.SeedRunResult> seedIdentities() {
        return ResponseEntity.ok(identitySeedService.seedFromFile());
    }
}
