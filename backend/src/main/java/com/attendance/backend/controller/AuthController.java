package com.attendance.backend.controller;

import com.attendance.backend.model.User;
import com.attendance.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        System.out.println("Login attempt for user: " + request.get("username"));
        try {
            return ResponseEntity.ok(authService.login(request.get("username"), request.get("password")));
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        authService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }
}
