package com.attendance.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("DEBUG: >>> [" + method + "] " + path);
        
        chain.doFilter(request, response);
        
        long duration = System.currentTimeMillis() - startTime;
        int status = response.getStatus();
        System.out.println("DEBUG: <<< [" + method + "] " + path + " - Status: " + status + " (" + duration + "ms)");
    }
}
