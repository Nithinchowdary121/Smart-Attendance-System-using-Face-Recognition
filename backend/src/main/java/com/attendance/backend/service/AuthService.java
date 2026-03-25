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

    public void register(User user) {
        System.out.println("Registering user: [" + user.getUsername() + "] with role: [" + user.getRole() + "]");
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            System.err.println("Registration FAILED: Username [" + user.getUsername() + "] already exists");
            throw new RuntimeException("Username already exists");
        }
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        System.out.println("User [" + user.getUsername() + "] saved to database successfully");

        // If it's a student, automatically create a student profile
        if ("USER".equals(user.getRole())) {
            System.out.println("Creating student profile for user: [" + user.getUsername() + "]");
            com.attendance.backend.model.Student student = new com.attendance.backend.model.Student();
            student.setName(user.getName() != null ? user.getName() : user.getUsername());
            student.setEmail(user.getEmail() != null ? user.getEmail() : user.getUsername());
            student.setRollNumber(user.getRollNumber() != null ? user.getRollNumber() : "TEMP-" + System.currentTimeMillis());
            studentRepository.save(student);
            System.out.println("Student profile created successfully for: [" + student.getEmail() + "]");
        }
    }
}
