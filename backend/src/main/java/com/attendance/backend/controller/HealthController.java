package com.attendance.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/healthz")
    public ResponseEntity<String> healthCheck() {
        // This endpoint will be available as soon as the web server starts,
        // allowing Render to mark the service as healthy while the rest of the app initializes.
        return ResponseEntity.ok("OK");
    }
}
