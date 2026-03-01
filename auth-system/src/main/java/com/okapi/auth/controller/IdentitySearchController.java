package com.okapi.auth.controller;

import com.okapi.auth.model.db.IdentityEntity;
import com.okapi.auth.model.db.RoleEntity;
import com.okapi.auth.repository.IdentityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Identity search endpoint for suggest boxes (e.g. pathologist assignment).
 * Requires authentication (any user) — frontend gates by permission.
 * Separate from /admin/identities which requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/identities")
public class IdentitySearchController {

    private final IdentityRepository identityRepository;

    public IdentitySearchController(IdentityRepository identityRepository) {
        this.identityRepository = identityRepository;
    }

    public record IdentitySearchResult(
            UUID identityId,
            String displayName,
            String displayShort,
            String email,
            List<String> roles) {

        public static IdentitySearchResult from(IdentityEntity entity) {
            List<String> roleNames = entity.getRoles().stream()
                    .map(RoleEntity::getName)
                    .sorted()
                    .toList();
            return new IdentitySearchResult(
                    entity.getIdentityId(),
                    entity.getDisplayName(),
                    entity.getDisplayShort(),
                    entity.getEmail(),
                    roleNames);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<IdentitySearchResult>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        int effectiveLimit = Math.min(Math.max(limit, 1), 50);

        List<IdentitySearchResult> results = identityRepository
                .searchByTerm(query.trim(), PageRequest.of(0, effectiveLimit))
                .getContent()
                .stream()
                .filter(IdentityEntity::isActive)
                .map(IdentitySearchResult::from)
                .toList();

        return ResponseEntity.ok(results);
    }
}
