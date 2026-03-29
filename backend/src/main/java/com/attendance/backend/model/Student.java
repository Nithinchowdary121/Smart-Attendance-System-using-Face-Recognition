package com.attendance.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

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

    @Lob
    @Column(columnDefinition="LONGBLOB")
    @JsonIgnore
    private byte[] faceData;

    public Student(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRollNumber() {
        return rollNumber;
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

    public byte[] getFaceData() {
        return faceData;
    }

    public void setFaceData(byte[] faceData) {
        this.faceData = faceData;
    }
}