package com.attendance.backend.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.attendance.backend.service.AttendanceService;
import com.attendance.backend.service.FaceRecognitionService;

@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final FaceRecognitionService FaceRecognitionService;
    private final AttendanceService attendanceService;

    public FaceController(FaceRecognitionService faceRecognitionService,
                          AttendanceService attendanceService) {
        this.FaceRecognitionService = faceRecognitionService;   
        this.attendanceService = attendanceService;
    }

    @PostMapping("/recognize")
    public String recognizeFace(@RequestBody java.util.Map<String, Object> request){
        String imageData = (String) request.get("image");
        Long subjectId = Long.valueOf(request.get("subjectId").toString());
        
        // Get logged-in user email from SecurityContext
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails)principal).getUsername();
        } else {
            email = principal.toString();
        }

        return attendanceService.markAttendance(imageData, subjectId, email);
    }
}
