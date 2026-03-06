package com.okapi.auth.controller;

import com.okapi.auth.dto.AdminDtos.FeedbackSubmitRequest;
import com.okapi.auth.model.Identity;
import com.okapi.auth.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final AdminService adminService;

    public FeedbackController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(
            @AuthenticationPrincipal Identity actor,
            @RequestBody FeedbackSubmitRequest request) {
        adminService.submitFeedback(actor, request);
        return ResponseEntity.ok().build();
    }
}
