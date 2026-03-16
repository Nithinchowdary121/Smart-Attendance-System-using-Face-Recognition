package com.attendance.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.attendance.backend.model.Student;

public interface StudentRepository extends JpaRepository<Student,Long> {
}