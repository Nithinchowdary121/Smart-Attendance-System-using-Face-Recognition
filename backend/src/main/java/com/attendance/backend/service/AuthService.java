package com.attendance.backend.service;

import com.attendance.backend.config.JwtUtils;
import com.attendance.backend.model.User;
import com.attendance.backend.repository.UserRepository;
import com.attendance.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentRepository studentRepository;

    public Map<String, String> login(String username, String password) {
        System.out.println("Starting authentication for user: [" + username + "]");
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            System.out.println("Authentication SUCCESS for user: [" + username + "]");
        } catch (Exception e) {
            System.out.println("Authentication FAILED for user: [" + username + "] - Error: " + e.getMessage());
            throw e;
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByUsername(username).orElseThrow();

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        return response;
    }

    @Transactional
    public void register(User user) {
        System.out.println("DEBUG: Registering user: [" + user.getUsername() + "] role: [" + user.getRole() + "] email: [" + user.getEmail() + "]");
        
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            System.err.println("DEBUG: Registration FAILED: Username [" + user.getUsername() + "] already exists");
            throw new RuntimeException("Username '" + user.getUsername() + "' already exists. Please login instead.");
        }

        try {
            // Default role if not provided
            if (user.getRole() == null || user.getRole().trim().isEmpty()) {
                user.setRole("STUDENT");
            }

            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            
            // Map email and name from username if missing (common for simple forms)
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) user.setEmail(user.getUsername());
            if (user.getName() == null || user.getName().trim().isEmpty()) user.setName(user.getUsername());

            User savedUser = userRepository.save(user);
            System.out.println("DEBUG: User [" + savedUser.getUsername() + "] saved with ID: " + savedUser.getId());

            // If it's a student, automatically create a student profile if it doesn't exist
            if ("STUDENT".equals(user.getRole()) || "USER".equals(user.getRole())) {
                user.setRole("STUDENT");
                
                System.out.println("DEBUG: Checking student profile for email: [" + user.getEmail() + "]");
                if (studentRepository.findByEmail(user.getEmail()).isEmpty()) {
                    com.attendance.backend.model.Student student = new com.attendance.backend.model.Student();
                    student.setName(user.getName());
                    student.setEmail(user.getEmail());
                    student.setRollNumber(user.getRollNumber() != null ? user.getRollNumber() : "REG-" + System.currentTimeMillis());
                    studentRepository.save(student);
                    System.out.println("DEBUG: Student profile created for: [" + student.getEmail() + "]");
                } else {
                    System.out.println("DEBUG: Student profile already exists for: [" + user.getEmail() + "]");
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Registration ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }
}
