package com.attendance.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @PostMapping
    public ResponseEntity<?> getChatResponse(@RequestBody Map<String, String> request) {
        String message = request.get("message").toLowerCase();
        String response;

        if (message.contains("attendance")) {
            response = "You can mark attendance in the 'Attendance' section using your face. Make sure your face is clearly visible to the camera.";
        } else if (message.contains("register") || message.contains("student")) {
            response = "Admins can register new students in the 'Students' section. You'll need their name, roll number, and a clear photo for face recognition.";
        } else if (message.contains("report")) {
            response = "Attendance reports are available in the 'Reports' section for Administrators. You can filter by date and export them as PDF.";
        } else if (message.contains("admin")) {
            response = "Administrators have access to the dashboard, student management, and detailed reports. Standard users can only mark attendance.";
        } else if (message.contains("hi") || message.contains("hello")) {
            response = "Hello! I'm your Smart Attendance Assistant. How can I help you today?";
        } else {
            response = "I'm here to help with the Smart Attendance System. You can ask about marking attendance, registering students, or generating reports.";
        }

        Map<String, String> res = new HashMap<>();
        res.put("response", response);
        return ResponseEntity.ok(res);
    }
}
