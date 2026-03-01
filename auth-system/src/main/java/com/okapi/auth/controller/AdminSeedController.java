package com.okapi.auth.controller;

import com.okapi.auth.service.seed.CaseAssignmentSeedService;
import com.okapi.auth.service.seed.CaseSeedService;
import com.okapi.auth.service.seed.IdentitySeedService;
import com.okapi.auth.service.seed.PatientSeedService;
import com.okapi.auth.service.seed.WorklistSeedService;
import com.okapi.auth.service.seed.WorklistSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seed")
public class AdminSeedController {

    private final IdentitySeedService identitySeedService;
    private final WorklistSeedService worklistSeedService;
    private final WorklistSyncService worklistSyncService;
    private final PatientSeedService patientSeedService;
    private final CaseSeedService caseSeedService;
    private final CaseAssignmentSeedService caseAssignmentSeedService;

    public AdminSeedController(
            IdentitySeedService identitySeedService,
            WorklistSeedService worklistSeedService,
            WorklistSyncService worklistSyncService,
            PatientSeedService patientSeedService,
            CaseSeedService caseSeedService,
            CaseAssignmentSeedService caseAssignmentSeedService) {
        this.identitySeedService = identitySeedService;
        this.worklistSeedService = worklistSeedService;
        this.worklistSyncService = worklistSyncService;
        this.patientSeedService = patientSeedService;
        this.caseSeedService = caseSeedService;
        this.caseAssignmentSeedService = caseAssignmentSeedService;
    }

    @PostMapping("/identities")
    public ResponseEntity<IdentitySeedService.SeedRunResult> seedIdentities() {
        return ResponseEntity.ok(identitySeedService.seedFromFile());
    }

    @PostMapping("/worklist")
    public ResponseEntity<WorklistSeedService.SeedRunResult> seedWorklist() {
        return ResponseEntity.ok(worklistSeedService.seedFromFile());
    }

    @PostMapping("/patients")
    public ResponseEntity<PatientSeedService.SeedRunResult> seedPatients() {
        return ResponseEntity.ok(patientSeedService.seedFromFile());
    }

    @PostMapping("/cases")
    public ResponseEntity<CaseSeedService.SeedRunResult> seedCases() {
        return ResponseEntity.ok(caseSeedService.seedFromFile());
    }

    @PostMapping("/case-assignments")
    public ResponseEntity<CaseAssignmentSeedService.SeedRunResult> seedCaseAssignments() {
        return ResponseEntity.ok(caseAssignmentSeedService.seedFromFile());
    }

    @PostMapping("/worklist-sync")
    public ResponseEntity<WorklistSyncService.SyncResult> syncWorklist() {
        return ResponseEntity.ok(worklistSyncService.syncFromCases());
    }
}
