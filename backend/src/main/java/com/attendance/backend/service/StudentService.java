package com.attendance.backend.service;

import com.attendance.backend.model.Student;
import com.attendance.backend.model.User;
import com.attendance.backend.repository.AttendanceRepository;
import com.attendance.backend.repository.StudentRepository;
import com.attendance.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Transactional
    public Student registerStudent(Student student, String base64Image) throws IOException {
        // Create user account for student using EMAIL as username
        if (userRepository.findByUsername(student.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + student.getEmail());
        }

        User user = new User();
        user.setUsername(student.getEmail());
        // Default password is the roll number
        user.setPassword(passwordEncoder.encode(student.getRollNumber()));
        user.setRole("STUDENT");
        userRepository.save(user);

        // Save initial student to get ID
        Student savedStudent = studentRepository.save(student);
        
        // Save face image and update student record with path
        String facePath = faceRecognitionService.saveStudentFace(savedStudent.getId(), base64Image);
        savedStudent.setFaceImagePath(facePath);
        
        return studentRepository.save(savedStudent);
    }

    @Transactional
    public Student updateStudent(Long id, Student studentDetails, String base64Image) throws IOException {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        // Update user account if email changed
        if (!student.getEmail().equals(studentDetails.getEmail())) {
            User user = userRepository.findByUsername(student.getEmail())
                    .orElseThrow(() -> new RuntimeException("User account not found for student email: " + student.getEmail()));
            
            // Check if new email already taken
            if (userRepository.findByUsername(studentDetails.getEmail()).isPresent()) {
                throw new RuntimeException("New email already exists: " + studentDetails.getEmail());
            }
            
            user.setUsername(studentDetails.getEmail());
            userRepository.save(user);
        }

        // Update password if roll number changed
        if (!student.getRollNumber().equals(studentDetails.getRollNumber())) {
            User user = userRepository.findByUsername(studentDetails.getEmail())
                    .orElseThrow(() -> new RuntimeException("User account not found for student email: " + studentDetails.getEmail()));
            user.setPassword(passwordEncoder.encode(studentDetails.getRollNumber()));
            userRepository.save(user);
        }

        student.setName(studentDetails.getName());
        student.setEmail(studentDetails.getEmail());
        student.setRollNumber(studentDetails.getRollNumber());
        
        if (base64Image != null && !base64Image.isEmpty()) {
            String facePath = faceRecognitionService.saveStudentFace(student.getId(), base64Image);
            student.setFaceImagePath(facePath);
        }
        
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        // Delete associated user account using EMAIL
        userRepository.findByUsername(student.getEmail())
                .ifPresent(userRepository::delete);

        // Delete associated attendance records first
        attendanceRepository.deleteByStudentId(id);
        
        // Delete the face image file
        if (student.getFaceImagePath() != null) {
            java.io.File file = new java.io.File(student.getFaceImagePath());
            if (file.exists()) {
                file.delete();
            }
        }
        
        studentRepository.delete(student);
        // Retrain model after deletion
        faceRecognitionService.trainModel();
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
}
