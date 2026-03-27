package com.attendance.backend.service;

import com.attendance.backend.model.Attendance;
import com.attendance.backend.model.Student;
import com.attendance.backend.model.User;
import com.attendance.backend.repository.AttendanceRepository;
import com.attendance.backend.repository.StudentRepository;
import com.attendance.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private UserRepository userRepository;

    public String markAttendance(String base64Image, Long subjectId, String currentUserEmailOrUsername) {
        // 1. First, find the User record by username (the login identifier)
        Optional<User> userOptional = userRepository.findByUsername(currentUserEmailOrUsername);
        
        if (userOptional.isEmpty()) {
            System.err.println("DEBUG: Attendance marking failed - No User found with identifier: " + currentUserEmailOrUsername);
            return "User profile not found for account: " + currentUserEmailOrUsername;
        }

        User user = userOptional.get();
        
        // 2. Find the Student record using the EMAIL linked to that User account
        // This ensures that even if a student creates their own account with a custom username,
        // we can still find their biometric profile via their unique email.
        Optional<Student> studentOptional = studentRepository.findByEmail(user.getEmail());

        if (studentOptional.isEmpty()) {
            System.err.println("DEBUG: Attendance marking failed - No student profile linked to email: " + user.getEmail());
            return "Student biometric profile not found for email: " + user.getEmail();
        }

        Student currentStudent = studentOptional.get();
        System.out.println("DEBUG: Attendance marking attempt for Student: " + currentStudent.getName() + " (ID: " + currentStudent.getId() + ")");
        
        Long recognizedStudentId = faceRecognitionService.recognizeStudent(base64Image);

        if (recognizedStudentId != null && recognizedStudentId.equals(currentStudent.getId())) {
            System.out.println("DEBUG: Face recognition successful for Student ID: " + recognizedStudentId);
            // Check if already marked for today and this subject
            Optional<Attendance> existing = attendanceRepository.findByStudentIdAndDateAndSubjectId(currentStudent.getId(), LocalDate.now(), subjectId);
            if (existing.isPresent()) {
                System.out.println("DEBUG: Attendance already marked for today for this subject.");
                return "Attendance already marked for " + currentStudent.getName() + " in this session.";
            }

            Attendance attendance = new Attendance();
            attendance.setStudentId(currentStudent.getId());
            attendance.setSubjectId(subjectId);
            attendance.setDate(LocalDate.now());
            attendance.setTime(LocalTime.now());
            attendance.setStatus("PRESENT");
            attendanceRepository.save(attendance);
            System.out.println("DEBUG: Attendance saved successfully for Student: " + currentStudent.getName());
            return "Attendance marked for " + currentStudent.getName();
        } else {
            System.err.println("DEBUG: Attendance REJECTED - Face does not match registered student ID: " + currentStudent.getId());
            return "Face not matched. Please try again with your registered identity.";
        }
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }
}
