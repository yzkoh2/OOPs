package com.editor;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.*;


import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;

import java.io.File;


public class BackgroundEditor {
    private CascadeClassifier faceDetector;
    
    public BackgroundEditor() {
        try {
            // Try to load the cascade file
            faceDetector = new CascadeClassifier();
            
            // Use a direct file path instead of trying to load from resources
            File cascadeFile = new File("./facialDetectionResources/haarcascade_frontalface_alt2.xml");
            if (cascadeFile.exists()) {
                faceDetector.load(cascadeFile.getAbsolutePath());
            } else {
                System.err.println("Cascade file not found. Face detection will be disabled.");
            }
            
            // Check if loading was successful
            if (faceDetector.empty()) {
                System.err.println("Failed to load cascade classifier");
                faceDetector = null;
            }
        } catch (Exception e) {
            System.err.println("Error initializing face detector: " + e.getMessage());
            faceDetector = null;
        }
    }
    
    public Mat removeBackground(Mat image) {
        // Detect faces
        Mat grayImage = new Mat();
        cvtColor(image, grayImage, COLOR_BGR2GRAY);
        
        // Create face mask
        Mat faceMask = Mat.zeros(image.size(), CV_8UC1).asMat();
        
        // Detect faces
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(grayImage, faces);
        
        // Draw faces on mask
        for (long i = 0; i < faces.size(); i++) {
            org.bytedeco.opencv.opencv_core.Rect face = faces.get(i);
            // Expand the face region slightly to ensure we capture the entire face
            int x = Math.max(0, face.x() - 10);
            int y = Math.max(0, face.y() - 10);
            int width = Math.min(image.cols() - x, face.width() + 20);
            int height = Math.min(image.rows() - y, face.height() + 20);
            
            rectangle(faceMask, new org.bytedeco.opencv.opencv_core.Rect(x, y, width, height), 
                     new Scalar(255, 255, 255, 255), FILLED, 8, 0);
        }
    
    // Proceed with background removal as before
    Mat gray = new Mat();
    cvtColor(image, gray, COLOR_BGR2GRAY);
    
    // Normalize the image
    normalize(gray, gray, 0, 255, NORM_MINMAX, -1, null);
    
    // Apply Gaussian blur and edge detection
    Mat blurred = new Mat();
    GaussianBlur(gray, blurred, new Size(5, 5), 0);
    
    Mat edges = new Mat();
    Canny(blurred, edges, 50, 150);
    
    // Dilate to close gaps
    Mat dilated = new Mat();
    Mat kernel = getStructuringElement(MORPH_RECT, new Size(3, 3));
    dilate(edges, dilated, kernel);
    
    // Find contours
    MatVector contours = new MatVector();
    Mat hierarchy = new Mat();
    findContours(dilated.clone(), contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    
    // Create mask
    Mat mask = Mat.zeros(image.size(), CV_8UC1).asMat();
    
    // Find the largest contour
    double maxArea = 0;
    int maxContourIdx = -1;
    for (int i = 0; i < contours.size(); i++) {
        double area = contourArea(contours.get(i));
        if (area > maxArea) {
            maxArea = area;
            maxContourIdx = i;
        }
    }
    
    // Draw the largest contour on the mask
    if (maxContourIdx >= 0) {
        drawContours(mask, contours, maxContourIdx, new Scalar(255, 255, 255, 255), -1, 8, null, 2, null);
    }
    
    // Combine with face mask to ensure faces are preserved
    bitwise_or(mask, faceMask, mask);
    
    // Apply mask to original image
    Mat result = new Mat();
    image.copyTo(result, mask);
    
    return result;
}

    
    public Mat removeShadows(Mat image) {
        // Convert to HSV
        Mat hsv = new Mat();
        cvtColor(image, hsv, COLOR_BGR2HSV);
        
        // Split channels
        MatVector channels = new MatVector(3);
        split(hsv, channels);
        
        // Get V channel
        Mat v = channels.get(2);
        
        // Create a scalar Mat for setTo operation
        Mat scalarMat = new Mat(1, 1, CV_8UC1, new Scalar(200));
        v.setTo(scalarMat);
        
        // Merge channels back
        merge(channels, hsv);
        
        // Convert back to BGR
        Mat result = new Mat();
        cvtColor(hsv, result, COLOR_HSV2BGR);
        
        return result;
    }
}
