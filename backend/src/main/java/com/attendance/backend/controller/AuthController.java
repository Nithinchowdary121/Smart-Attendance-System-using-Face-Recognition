package com.attendance.backend.controller;

import com.attendance.backend.model.User;
import com.attendance.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        System.out.println("Login attempt for user: [" + username + "]");
        try {
            Map<String, String> response = authService.login(username, password);
            System.out.println("Login successful for user: [" + username + "]");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Login failed for user: [" + username + "] - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid credentials: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        System.out.println("DEBUG: AuthController.register called for: [" + user.getUsername() + "]");
        try {
            authService.register(user);
            System.out.println("DEBUG: AuthController.register SUCCESS for: [" + user.getUsername() + "]");
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            System.err.println("DEBUG: AuthController.register FAILED for: [" + user.getUsername() + "] - Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
