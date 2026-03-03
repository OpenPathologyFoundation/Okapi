package com.okapi.auth.controller;

import com.okapi.auth.service.seed.CaseAssignmentSeedService;
import com.okapi.auth.service.seed.CaseSeedService;
import com.okapi.auth.service.seed.EduCaseSeedService;
import com.okapi.auth.service.seed.EduCuratorSeedService;
import com.okapi.auth.service.seed.IdentitySeedService;
import com.okapi.auth.service.seed.PatientSeedService;
import com.okapi.auth.service.seed.WorklistSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seed")
public class AdminSeedController {

    private final IdentitySeedService identitySeedService;
    private final WorklistSyncService worklistSyncService;
    private final PatientSeedService patientSeedService;
    private final CaseSeedService caseSeedService;
    private final CaseAssignmentSeedService caseAssignmentSeedService;
    private final EduCaseSeedService eduCaseSeedService;
    private final EduCuratorSeedService eduCuratorSeedService;

    public AdminSeedController(
            IdentitySeedService identitySeedService,
            WorklistSyncService worklistSyncService,
            PatientSeedService patientSeedService,
            CaseSeedService caseSeedService,
            CaseAssignmentSeedService caseAssignmentSeedService,
            EduCaseSeedService eduCaseSeedService,
            EduCuratorSeedService eduCuratorSeedService) {
        this.identitySeedService = identitySeedService;
        this.worklistSyncService = worklistSyncService;
        this.patientSeedService = patientSeedService;
        this.caseSeedService = caseSeedService;
        this.caseAssignmentSeedService = caseAssignmentSeedService;
        this.eduCaseSeedService = eduCaseSeedService;
        this.eduCuratorSeedService = eduCuratorSeedService;
    }

    @PostMapping("/identities")
    public ResponseEntity<IdentitySeedService.SeedRunResult> seedIdentities() {
        return ResponseEntity.ok(identitySeedService.seedFromFile());
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

    @PostMapping("/edu-cases")
    public ResponseEntity<EduCaseSeedService.SeedRunResult> seedEduCases() {
        return ResponseEntity.ok(eduCaseSeedService.seedFromFile());
    }

    @PostMapping("/edu-curators")
    public ResponseEntity<EduCuratorSeedService.SeedRunResult> seedEduCurators() {
        return ResponseEntity.ok(eduCuratorSeedService.seedFromFile());
    }
}
