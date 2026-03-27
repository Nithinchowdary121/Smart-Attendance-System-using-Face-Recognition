package com.attendance.backend.controller;

import com.attendance.backend.model.Student;
import com.attendance.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(@RequestBody Map<String, Object> request) {
        try {
            Student student = new Student();
            student.setName((String) request.get("name"));
            student.setEmail((String) request.get("email"));
            student.setRollNumber((String) request.get("rollNumber"));
            
            String image = (String) request.get("image");
            return ResponseEntity.ok(studentService.registerStudent(student, image));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Student student = new Student();
            student.setName((String) request.get("name"));
            student.setEmail((String) request.get("email"));
            student.setRollNumber((String) request.get("rollNumber"));
            
            String image = (String) request.get("image");
            return ResponseEntity.ok(studentService.updateStudent(id, student, image));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Update failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);
            return ResponseEntity.ok("Student deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Deletion failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }
}
