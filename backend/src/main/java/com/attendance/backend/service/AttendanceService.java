package com.attendance.backend.service;

import com.attendance.backend.model.Attendance;
import com.attendance.backend.model.Student;
import com.attendance.backend.repository.AttendanceRepository;
import com.attendance.backend.repository.StudentRepository;
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

    public String markAttendance(String base64Image, Long subjectId, String currentUserEmail) {
        // Find the student by the logged-in user's email or username
        Optional<Student> studentOptional = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equalsIgnoreCase(currentUserEmail) || s.getName().equalsIgnoreCase(currentUserEmail))
                .findFirst();

        if (studentOptional.isEmpty()) {
            // If the current user is an admin, they might be using the "Any student" recognition mode.
            // But the user requested verification for the specified person.
            // Let's check if the recognized face matches ANY student first if it's an admin.
            // However, based on the prompt, it seems we want a specific verification.
            return "Student profile not found for email: " + currentUserEmail;
        }

        Student currentStudent = studentOptional.get();
        Long recognizedStudentId = faceRecognitionService.recognizeStudent(base64Image);

        if (recognizedStudentId != null && recognizedStudentId.equals(currentStudent.getId())) {
            // Check if already marked for today and this subject
            Optional<Attendance> existing = attendanceRepository.findByStudentIdAndDateAndSubjectId(currentStudent.getId(), LocalDate.now(), subjectId);
            if (existing.isPresent()) {
                return "Attendance already marked for " + currentStudent.getName() + " in this session.";
            }

            Attendance attendance = new Attendance();
            attendance.setStudentId(currentStudent.getId());
            attendance.setSubjectId(subjectId);
            attendance.setDate(LocalDate.now());
            attendance.setTime(LocalTime.now());
            attendance.setStatus("PRESENT");
            attendanceRepository.save(attendance);
            return "Attendance marked for " + currentStudent.getName();
        } else {
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
