package com.okapi.auth.controller;

import com.okapi.auth.model.Identity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Annotation persistence API stubs.
 *
 * These endpoints define the contract for viewer annotation persistence
 * but are not yet implemented. They return 501 Not Implemented with
 * descriptive messages so the viewer can detect and handle accordingly.
 *
 * Future implementation will store GeoJSON annotation geometries linked
 * to cases and slides, with full audit trail integration.
 */
@RestController
@RequestMapping("/api/cases/{accession}/annotations")
public class AnnotationController {

    private static final Map<String, String> NOT_IMPLEMENTED =
            Map.of("error", "not_implemented", "message", "Annotation persistence not yet implemented");

    @GetMapping
    public ResponseEntity<Map<String, String>> listAnnotations(
            @PathVariable String accession,
            @RequestParam(required = false) String slideId,
            @AuthenticationPrincipal Identity identity) {
        if (identity == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.status(501).body(NOT_IMPLEMENTED);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createAnnotation(
            @PathVariable String accession,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Identity identity) {
        if (identity == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.status(501).body(NOT_IMPLEMENTED);
    }

    @PutMapping("/{annotationId}")
    public ResponseEntity<Map<String, String>> updateAnnotation(
            @PathVariable String accession,
            @PathVariable UUID annotationId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Identity identity) {
        if (identity == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.status(501).body(NOT_IMPLEMENTED);
    }

    @DeleteMapping("/{annotationId}")
    public ResponseEntity<Map<String, String>> deleteAnnotation(
            @PathVariable String accession,
            @PathVariable UUID annotationId,
            @AuthenticationPrincipal Identity identity) {
        if (identity == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.status(501).body(NOT_IMPLEMENTED);
    }
}
