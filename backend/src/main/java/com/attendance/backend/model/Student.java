package com.attendance.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String rollNumber;

    private String FaceImagePath;

    public Student(){}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getFaceImagePath() {
        return FaceImagePath;
    }

    public void setFaceImagePath(String faceImagePath) {
        this.FaceImagePath = faceImagePath;
    }
}