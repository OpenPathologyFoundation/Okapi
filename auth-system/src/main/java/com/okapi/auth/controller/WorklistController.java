package com.okapi.auth.controller;

import com.okapi.auth.dto.WorklistDtos.*;
import com.okapi.auth.model.Identity;
import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.service.WorklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/worklist")
public class WorklistController {

    private final WorklistService worklistService;
    private final IdentityRepository identityRepository;

    public WorklistController(WorklistService worklistService, IdentityRepository identityRepository) {
        this.worklistService = worklistService;
        this.identityRepository = identityRepository;
    }

    @GetMapping
    public ResponseEntity<WorklistPageResponse> getWorklist(
            @RequestParam(required = false) List<String> services,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> priorities,
            @RequestParam(required = false) UUID assignedToIdentityId,
            @RequestParam(required = false, defaultValue = "false") Boolean myCasesOnly,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "caseDate") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize,
            @AuthenticationPrincipal Object principal) {

        UUID currentUserId = resolveCurrentUserId(principal);

        WorklistFilterRequest filter = new WorklistFilterRequest(
                services, statuses, priorities, assignedToIdentityId, myCasesOnly,
                search, fromDate, toDate, sortBy, sortDir, page, pageSize);

        WorklistPageResponse response = worklistService.getWorklist(filter, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accessionNumber}")
    public ResponseEntity<WorklistItemResponse> getByAccessionNumber(
            @PathVariable String accessionNumber) {
        WorklistItemResponse item = worklistService.getByAccessionNumber(accessionNumber);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    private UUID resolveCurrentUserId(Object principal) {
        if (principal instanceof Identity identity) {
            return identityRepository
                    .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                    .map(IdentityEntity::getIdentityId)
                    .orElse(null);
        }
        return null;
    }
}
