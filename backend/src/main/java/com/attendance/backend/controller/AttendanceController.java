package com.attendance.backend.controller;

import com.attendance.backend.model.Attendance;
import com.attendance.backend.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/mark")
    public ResponseEntity<String> markAttendance(@RequestBody Map<String, Object> request) {
        String base64Image = (String) request.get("image");
        Long subjectId = Long.valueOf(request.get("subjectId").toString());
        
        // Get logged-in user email from SecurityContext
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails)principal).getUsername();
        } else {
            email = principal.toString();
        }

        String result = attendanceService.markAttendance(base64Image, subjectId, email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/report")
    public ResponseEntity<List<Attendance>> getReport(@RequestParam(required = false) String date) {
        if (date != null) {
            return ResponseEntity.ok(attendanceService.getAttendanceByDate(LocalDate.parse(date)));
        }
        return ResponseEntity.ok(attendanceService.getAllAttendance());
    }
}
