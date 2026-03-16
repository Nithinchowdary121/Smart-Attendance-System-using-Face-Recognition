package com.attendance.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.attendance.backend.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface AttendanceRepository extends JpaRepository<Attendance,Long> {
     Optional<Attendance> findByStudentIdAndDateAndSubjectId(Long studentId, LocalDate date, Long subjectId);
     List<Attendance> findByDate(LocalDate date);
     
     @Transactional
     void deleteByStudentId(Long studentId);
 }