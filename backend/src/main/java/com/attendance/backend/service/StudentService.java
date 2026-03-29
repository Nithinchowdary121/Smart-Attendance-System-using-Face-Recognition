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
        System.out.println("Registering student: " + student.getName() + " (" + student.getEmail() + ")");
        
        // 1. Ensure a User account exists for this student
        // When an Admin creates a student profile, we must ensure the User role is "STUDENT"
        User user = userRepository.findByUsername(student.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(student.getEmail());
                    newUser.setRole("STUDENT"); // ALWAYS STUDENT when created via StudentService
                    return newUser;
                });

        user.setName(student.getName());
        user.setEmail(student.getEmail());
        user.setRollNumber(student.getRollNumber());
        user.setRole("STUDENT"); // Force role update if user existed with wrong role
        
        // If it's a new account, set default password as roll number
        if (user.getId() == null) {
            user.setPassword(passwordEncoder.encode(student.getRollNumber()));
        }
        
        userRepository.save(user);
        System.out.println("User account linked/created for student");

        // 2. Check if a Student biometric record already exists for this email
        Student studentToSave = studentRepository.findByEmail(student.getEmail())
                .orElse(student);
        
        studentToSave.setName(student.getName());
        studentToSave.setEmail(student.getEmail());
        studentToSave.setRollNumber(student.getRollNumber());

        // Save initial student to get ID
        Student savedStudent = studentRepository.save(studentToSave);
        System.out.println("Student biometric record saved with ID: " + savedStudent.getId());
        
        try {
            // 3. Save face image data to DATABASE (persistent on Render)
            byte[] faceBytes = faceRecognitionService.decodeBase64Image(base64Image);
            if (faceBytes != null) {
                savedStudent.setFaceData(faceBytes);
                System.out.println("DEBUG: Face bytes saved to student record (DB persistence)");
            }

            // 4. Save face image to disk (ephemeral path for backward compatibility)
            String facePath = faceRecognitionService.saveStudentFace(savedStudent.getId(), base64Image);
            savedStudent.setFaceImagePath(facePath);
            
            studentRepository.saveAndFlush(savedStudent); // Force save to DB
            System.out.println("Face image saved to disk: " + facePath);
            
            // Retrain the model AFTER the database record is fully saved and flushed
            System.out.println("Retraining model with new student data...");
            faceRecognitionService.trainModel();
        } catch (Exception e) {
            System.err.println("Error saving face image: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process face image: " + e.getMessage());
        }
        
        return studentRepository.save(savedStudent);
    }

    @Transactional
    public Student updateStudent(Long id, Student studentDetails, String base64Image) throws IOException {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        // Find or create user account
        User user = userRepository.findByUsername(student.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(student.getEmail());
                    newUser.setRole("STUDENT");
                    return newUser;
                });

        // Check if new email is already taken by ANOTHER user
        if (!student.getEmail().equals(studentDetails.getEmail())) {
            if (userRepository.findByUsername(studentDetails.getEmail()).isPresent()) {
                throw new RuntimeException("New email already exists: " + studentDetails.getEmail());
            }
        }

        // Sync user account with new student details
        user.setUsername(studentDetails.getEmail());
        user.setEmail(studentDetails.getEmail());
        user.setName(studentDetails.getName());
        user.setRollNumber(studentDetails.getRollNumber());
        
        // Update password if roll number changed (or if it's a new user account)
        if (user.getId() == null || !student.getRollNumber().equals(studentDetails.getRollNumber())) {
            user.setPassword(passwordEncoder.encode(studentDetails.getRollNumber()));
        }
        
        userRepository.save(user);

        // Update student record
        student.setName(studentDetails.getName());
        student.setEmail(studentDetails.getEmail());
        student.setRollNumber(studentDetails.getRollNumber());
        
        if (base64Image != null && !base64Image.isEmpty()) {
            // Update database blob for persistence
            byte[] faceBytes = faceRecognitionService.decodeBase64Image(base64Image);
            if (faceBytes != null) {
                student.setFaceData(faceBytes);
                System.out.println("DEBUG: Updated student face data in DB.");
            }
            
            // Update file system path for compatibility
            String facePath = faceRecognitionService.saveStudentFace(student.getId(), base64Image);
            student.setFaceImagePath(facePath);
            
            studentRepository.saveAndFlush(student); // Force save before training
            System.out.println("Retraining model after student update...");
            faceRecognitionService.trainModel();
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
