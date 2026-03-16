package com.attendance.backend.service;

import com.attendance.backend.config.JwtUtils;
import com.attendance.backend.model.User;
import com.attendance.backend.repository.UserRepository;
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
        return response;
    }

    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }
}
