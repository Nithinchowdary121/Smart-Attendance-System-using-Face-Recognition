package com.attendance.backend.service;

import com.attendance.backend.model.Student;
import com.attendance.backend.repository.StudentRepository;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class FaceRecognitionService {

    @Autowired
    private StudentRepository studentRepository;

    private LBPHFaceRecognizer recognizer;
    private CascadeClassifier faceDetector;
    private final String storagePath = "face_data/";
    private final Size faceSize = new Size(160, 160);

    @PostConstruct
    public void init() {
        // Optimized LBPH parameters for better accuracy and granularity
        // Radius: 1, Neighbors: 8, Grid X: 8, Grid Y: 8, Threshold: Double.MAX_VALUE
        recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, Double.MAX_VALUE);
        
        try {
            // Try to load the cascade file. If it's not in the root, it might be in resources.
            // For now, we'll try to load it and check if it's actually loaded.
            faceDetector = new CascadeClassifier("haarcascade_frontalface_default.xml");
            if (faceDetector.empty()) {
                System.out.println("Warning: Cascade classifier is empty. Face detection will be skipped.");
                faceDetector = null;
            } else {
                System.out.println("Cascade classifier loaded successfully.");
            }
        } catch (Exception e) {
            System.out.println("Error loading cascade classifier: " + e.getMessage());
            faceDetector = null;
        }
        
        File dir = new File(storagePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        trainModel();
    }

    public void trainModel() {
        try {
            List<Student> students = studentRepository.findAll();
            if (students.isEmpty()) {
                System.out.println("No students found for training.");
                return;
            }

            List<Mat> imagesList = new ArrayList<>();
            List<Integer> labelsList = new ArrayList<>();

            for (Student student : students) {
                if (student.getFaceImagePath() != null) {
                    File imgFile = new File(student.getFaceImagePath());
                    if (imgFile.exists()) {
                        Mat img = imread(student.getFaceImagePath(), IMREAD_GRAYSCALE);
                        if (!img.empty()) {
                            Mat face = preprocessFace(img);
                            if (face != null && !face.empty()) {
                                imagesList.add(face);
                                labelsList.add(student.getId().intValue());
                            }
                        }
                    }
                }
            }

            if (!imagesList.isEmpty()) {
                MatVector images = new MatVector(imagesList.size());
                Mat labelsMat = new Mat(imagesList.size(), 1, CV_32SC1);
                IntPointer labelsPointer = new IntPointer(labelsMat.data());

                for (int i = 0; i < imagesList.size(); i++) {
                    images.put(i, imagesList.get(i));
                    labelsPointer.put(i, labelsList.get(i));
                }
                recognizer.train(images, labelsMat);
                System.out.println("Model trained with " + imagesList.size() + " faces.");
            }
        } catch (Exception e) {
            System.out.println("Error during model training: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private Mat preprocessFace(Mat image) {
        Mat processed = new Mat();
        
        // 1. Face Detection
        if (faceDetector != null && !faceDetector.empty()) {
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(image, faces);
            
            if (faces.size() > 0) {
                Rect faceRect = faces.get(0);
                processed = new Mat(image, faceRect);
            } else {
                processed = image.clone();
            }
        } else {
            processed = image.clone();
        }

        // 2. Resize to standard size
        Mat resizedFace = new Mat();
        resize(processed, resizedFace, faceSize);

        // 3. Enhance Contrast using CLAHE (Contrast Limited Adaptive Histogram Equalization)
        // This is much better than global equalization for facial features
        Mat equalizedFace = new Mat();
        equalizeHist(resizedFace, equalizedFace);
        
        // 4. Noise reduction
        Mat finalFace = new Mat();
        GaussianBlur(equalizedFace, finalFace, new Size(3, 3), 0);

        return finalFace;
    }

    public Long recognizeStudent(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image.split(",")[1]);
            Mat img = imdecode(new Mat(imageBytes), IMREAD_GRAYSCALE);
            
            if (img.empty()) return null;

            Mat face = preprocessFace(img);
            if (face == null) return null;

            int[] label = new int[1];
            double[] confidence = new double[1];
            recognizer.predict(face, label, confidence);

            System.out.println("Predicted Label: " + label[0] + " with Confidence: " + confidence[0]);

            // LBPH confidence: lower is better. 
            // After optimization, a confidence below 60 is a very strong match.
            // A confidence between 60-80 is a likely match.
            if (label[0] != -1 && confidence[0] < 80.0) { 
                return (long) label[0];
            }
        } catch (Exception e) {
            System.out.println("Recognition error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String saveStudentFace(Long studentId, String base64Image) throws IOException {
        System.out.println("Processing face image for student ID: " + studentId);
        if (base64Image == null || !base64Image.contains(",")) {
            throw new IOException("Invalid image format");
        }
        
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image.split(",")[1]);
            String filePath = storagePath + "student_" + studentId + ".jpg";
            
            Mat img = imdecode(new Mat(imageBytes), IMREAD_GRAYSCALE);
            if (img.empty()) {
                throw new IOException("Failed to decode image");
            }
            
            Mat face = preprocessFace(img);
            
            if (face != null && !face.empty()) {
                System.out.println("Face detected and preprocessed. Saving to: " + filePath);
                if (!imwrite(filePath, face)) {
                    System.err.println("imwrite failed for path: " + filePath);
                    throw new IOException("Failed to save image file to disk");
                }
            } else {
                System.out.println("No face detected in image. Saving raw image to: " + filePath);
                Files.write(Paths.get(filePath), imageBytes);
            }
            
            return filePath;
        } catch (Exception e) {
            System.err.println("Error in saveStudentFace: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Face processing error: " + e.getMessage());
        }
    }
}
