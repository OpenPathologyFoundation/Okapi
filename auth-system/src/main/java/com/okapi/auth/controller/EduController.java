package com.okapi.auth.controller;

import com.okapi.auth.dto.EduDtos.*;
import com.okapi.auth.model.Identity;
import com.okapi.auth.repository.IdentityRepository;
import com.okapi.auth.service.EduCaseService;
import com.okapi.auth.service.EduCollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/edu")
public class EduController {

    private final EduCaseService eduCaseService;
    private final EduCollectionService eduCollectionService;
    private final IdentityRepository identityRepository;

    public EduController(
            EduCaseService eduCaseService,
            EduCollectionService eduCollectionService,
            IdentityRepository identityRepository) {
        this.eduCaseService = eduCaseService;
        this.eduCollectionService = eduCollectionService;
        this.identityRepository = identityRepository;
    }

    @GetMapping("/cases")
    public ResponseEntity<EduPageResponse<EduCaseListItem>> listCases(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String anatomicSite,
            @RequestParam(required = false) String specimenType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String stain,
            @RequestParam(required = false) UUID curatorId,
            @RequestParam(required = false) UUID collectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "ingestedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize) {

        EduSearchRequest request = new EduSearchRequest(
                query, anatomicSite, specimenType, difficulty, stain,
                curatorId, collectionId, status, sortBy, sortDir, page, pageSize);

        return ResponseEntity.ok(eduCaseService.listCases(request));
    }

    @GetMapping("/cases/{accession}")
    public ResponseEntity<EduCaseDetail> getCaseDetail(@PathVariable String accession) {
        EduCaseDetail detail = eduCaseService.getCaseDetail(accession);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/cases/{accession}/curators")
    public ResponseEntity<List<EduCuratorResponse>> getCurators(@PathVariable String accession) {
        return ResponseEntity.ok(eduCaseService.getCurators(accession));
    }

    @GetMapping("/collections")
    public ResponseEntity<List<EduNamedCollectionResponse>> listCollections(
            @AuthenticationPrincipal Object principal) {
        UUID currentUserId = resolveCurrentUserId(principal);
        return ResponseEntity.ok(eduCollectionService.listCollections(currentUserId));
    }

    @GetMapping("/collections/{id}")
    public ResponseEntity<EduNamedCollectionDetail> getCollectionDetail(@PathVariable UUID id) {
        EduNamedCollectionDetail detail = eduCollectionService.getCollectionDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/facets")
    public ResponseEntity<EduFacetCounts> getFacets() {
        // Use an empty search to get facets
        EduSearchRequest request = new EduSearchRequest(
                null, null, null, null, null, null, null, null, null, null, 0, 1);
        EduPageResponse<EduCaseListItem> response = eduCaseService.listCases(request);
        return ResponseEntity.ok(response.facets());
    }

    private UUID resolveCurrentUserId(Object principal) {
        if (principal instanceof Identity identity) {
            return identityRepository
                    .findByProviderIdAndExternalSubject(identity.getProviderId(), identity.getExternalSubject())
                    .map(e -> e.getIdentityId())
                    .orElse(null);
        }
        return null;
    }
}
