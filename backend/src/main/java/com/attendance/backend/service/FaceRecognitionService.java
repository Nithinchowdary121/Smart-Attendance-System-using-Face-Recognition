package com.attendance.backend.service;

import com.attendance.backend.model.Student;
import com.attendance.backend.repository.StudentRepository;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
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
        try {
            // Optimized LBPH parameters for better accuracy and granularity
            // Radius: 1, Neighbors: 8, Grid X: 8, Grid Y: 8, Threshold: Double.MAX_VALUE
            recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, Double.MAX_VALUE);
            System.out.println("LBPH Face Recognizer initialized successfully.");
        } catch (Throwable e) {
            System.err.println("CRITICAL WARNING: Failed to initialize LBPH Face Recognizer. Native library issue? " + e.getMessage());
            // Application continues to start, but face recognition will be disabled.
            recognizer = null;
        }
        
        File dir = new File(storagePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // Moved heavy classifier loading and model training to ApplicationReadyEvent
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("Application is ready. Initializing heavy components in background...");
        loadCascadeClassifier();
        trainModel();
    }

    private void loadCascadeClassifier() {
        try {
            // Try multiple locations for the cascade file
            String[] possiblePaths = {
                "haarcascade_frontalface_default.xml",
                "backend/haarcascade_frontalface_default.xml",
                "src/main/resources/haarcascade_frontalface_default.xml",
                new File(System.getProperty("user.dir"), "haarcascade_frontalface_default.xml").getAbsolutePath()
            };

            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists()) {
                    faceDetector = new CascadeClassifier(file.getAbsolutePath());
                    if (!faceDetector.empty()) {
                        System.out.println("Cascade classifier loaded successfully from: " + file.getAbsolutePath());
                        break;
                    }
                }
            }

            if (faceDetector == null || faceDetector.empty()) {
                System.out.println("Warning: Cascade classifier is empty. Face detection will be skipped.");
                faceDetector = null;
            }
        } catch (Exception e) {
            System.out.println("Error loading cascade classifier: " + e.getMessage());
            faceDetector = null;
        }
    }

    public void trainModel() {
        if (recognizer == null) {
            System.err.println("Model training skipped: Face recognizer is not initialized (native library issue?).");
            return;
        }
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
            // Use slightly more lenient detection parameters
            faceDetector.detectMultiScale(image, faces, 1.1, 3, 0, new Size(30, 30), new Size(500, 500));
            
            if (faces.size() > 0) {
                Rect faceRect = faces.get(0);
                // Expand the crop slightly to include more context (10% padding)
                int paddingX = (int) (faceRect.width() * 0.1);
                int paddingY = (int) (faceRect.height() * 0.1);
                
                int x = Math.max(0, faceRect.x() - paddingX);
                int y = Math.max(0, faceRect.y() - paddingY);
                int w = Math.min(image.cols() - x, faceRect.width() + 2 * paddingX);
                int h = Math.min(image.rows() - y, faceRect.height() + 2 * paddingY);
                
                processed = new Mat(image, new Rect(x, y, w, h));
                System.out.println("DEBUG: Face detected and cropped with padding.");
            } else {
                System.out.println("DEBUG: No face detected, using full image.");
                processed = image.clone();
            }
        } else {
            processed = image.clone();
        }

        // 2. Resize to standard size
        Mat resizedFace = new Mat();
        resize(processed, resizedFace, faceSize);

        // 3. Enhance Contrast using CLAHE (Contrast Limited Adaptive Histogram Equalization)
        // CLAHE is better for varying lighting conditions
        Mat equalizedFace = new Mat();
        equalizeHist(resizedFace, equalizedFace);
        
        // 4. Noise reduction
        Mat finalFace = new Mat();
        GaussianBlur(equalizedFace, finalFace, new Size(3, 3), 0);

        return finalFace;
    }

    public Long recognizeStudent(String base64Image) {
        if (recognizer == null) {
            System.err.println("DEBUG: Recognition skipped: Face recognizer is not initialized (native library issue?).");
            return null;
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image.split(",")[1]);
            Mat img = imdecode(new Mat(imageBytes), IMREAD_GRAYSCALE);
            
            if (img.empty()) {
                System.err.println("DEBUG: Recognition failed - Decoded image is empty.");
                return null;
            }

            Mat face = preprocessFace(img);
            if (face == null || face.empty()) {
                System.err.println("DEBUG: Recognition failed - Preprocessed face is empty.");
                return null;
            }

            int[] label = new int[1];
            double[] confidence = new double[1];
            
            // Log prediction attempt
            System.out.println("DEBUG: Starting face prediction...");
            recognizer.predict(face, label, confidence);

            System.out.println("DEBUG: Predicted Label: " + label[0] + " with Confidence: " + confidence[0]);

            // LBPH confidence: lower is better. 
            // - Strong Match: < 60
            // - Normal Match: 60 - 95
            // - Uncertain/Likely False: > 120
            // Increased threshold slightly to 125.0 to be more lenient in varied lighting
            if (label[0] != -1 && confidence[0] < 125.0) { 
                System.out.println("DEBUG: Face MATCHED (Confidence " + confidence[0] + " is within safe threshold < 125.0)");
                return (long) label[0];
            } else {
                System.out.println("DEBUG: Face REJECTED (Confidence " + confidence[0] + " is too high/unsafe or Label is -1)");
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Recognition error: " + e.getMessage());
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
            
            // Save RAW image to disk
            // We only preprocess in trainModel() and recognizeStudent()
            // to ensure consistency and avoid double preprocessing
            System.out.println("Saving raw student image to: " + filePath);
            Files.write(Paths.get(filePath), imageBytes);
            
            return filePath;
        } catch (Exception e) {
            System.err.println("Error in saveStudentFace: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Face storage error: " + e.getMessage());
        }
    }
}
