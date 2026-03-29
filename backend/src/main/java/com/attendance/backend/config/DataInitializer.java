package com.attendance.backend.config;

import com.attendance.backend.model.Student;
import com.attendance.backend.model.Subject;
import com.attendance.backend.model.User;
import com.attendance.backend.repository.StudentRepository;
import com.attendance.backend.repository.SubjectRepository;
import com.attendance.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("Data Initializer started in background...");
        try {
            initData();
        } catch (Exception e) {
            System.err.println("Error initializing data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initData() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("Admin user created: admin / admin123");
        }

        // Create individual accounts for existing students if missing or using old format
        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            // Ensure account exists using EMAIL as username
            if (userRepository.findByUsername(student.getEmail()).isEmpty()) {
                User user = new User();
                user.setUsername(student.getEmail());
                user.setEmail(student.getEmail());
                user.setName(student.getName());
                user.setRollNumber(student.getRollNumber());
                user.setPassword(passwordEncoder.encode(student.getRollNumber()));
                user.setRole("STUDENT");
                userRepository.save(user);
                System.out.println("User account synchronized for student: " + student.getEmail());
            }
        }

        if (subjectRepository.count() == 0) {
            String[][] subjects = {
                {"Computer Science", "CS101"},
                {"Mathematics", "MATH202"},
                {"Physics", "PHY303"},
                {"Artificial Intelligence", "AI404"}
            };
            for (String[] s : subjects) {
                Subject subject = new Subject();
                subject.setName(s[0]);
                subject.setCode(s[1]);
                subjectRepository.save(subject);
            }
            System.out.println("Default subjects created.");
        }
    }
}
