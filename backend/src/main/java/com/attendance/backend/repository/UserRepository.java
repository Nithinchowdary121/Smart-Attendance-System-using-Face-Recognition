package com.attendance.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.attendance.backend.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByUsername(String username);

}