package com.okapi.auth.controller;

import com.okapi.auth.service.seed.IdentitySeedService;
import com.okapi.auth.service.seed.WorklistSeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seed")
public class AdminSeedController {

    private final IdentitySeedService identitySeedService;
    private final WorklistSeedService worklistSeedService;

    public AdminSeedController(
            IdentitySeedService identitySeedService,
            WorklistSeedService worklistSeedService) {
        this.identitySeedService = identitySeedService;
        this.worklistSeedService = worklistSeedService;
    }

    @PostMapping("/identities")
    public ResponseEntity<IdentitySeedService.SeedRunResult> seedIdentities() {
        return ResponseEntity.ok(identitySeedService.seedFromFile());
    }

    @PostMapping("/worklist")
    public ResponseEntity<WorklistSeedService.SeedRunResult> seedWorklist() {
        return ResponseEntity.ok(worklistSeedService.seedFromFile());
    }
}
