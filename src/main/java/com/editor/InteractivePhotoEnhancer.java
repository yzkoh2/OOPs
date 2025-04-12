package com.editor;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

/**
 * Interactive photo enhancer that applies adjustments based on user slider inputs
 */
public class InteractivePhotoEnhancer {

    private static final String FACE_CASCADE_PATH = "facialDetectionResources/haarcascade_frontalface_default.xml";
    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    private static CascadeClassifier faceDetector;
    
    /**
     * Initialize the face detector
     */
    private static synchronized void initFaceDetector() throws Exception {
        if (faceDetector == null || faceDetector.isNull()) {
            faceDetector = new CascadeClassifier(FACE_CASCADE_PATH);
            if (faceDetector.empty()) {
                throw new RuntimeException("Failed to load face cascade classifier from " + FACE_CASCADE_PATH);
            }
        }
    }
    
    /**
     * Enhance a photo frame with specified parameters
     * 
     * @param originalFrame The input frame to enhance
     * @param contrast Contrast multiplier (1.0 = no change)
     * @param brightness Brightness adjustment (-100 to 100)
     * @param smoothingLevel Smoothing level (0 to 30)
     * @return The enhanced frame
     */
    public static Frame enhance(Frame originalFrame, double contrast, double brightness, int smoothingLevel) throws Exception {
        if (originalFrame == null) {
            throw new IllegalArgumentException("Original frame is null.");
        }
        
        // Convert Frame to Mat
        Mat inputMat = matConverter.convert(originalFrame);
        if (inputMat == null || inputMat.empty()) {
            throw new RuntimeException("Image data is empty or invalid.");
        }
        
        // Clone to avoid modifying original
        Mat outputMat = inputMat.clone();
        
        // Apply brightness and contrast adjustments
        outputMat.convertTo(outputMat, -1, contrast, brightness);
        
        // Apply face-specific smoothing if requested
        if (smoothingLevel > 0) {
            try {
                applySkinSmoothing(outputMat, smoothingLevel);
            } catch (Exception e) {
                System.err.println("Warning: Face smoothing failed: " + e.getMessage());
                // Continue with other enhancements even if face detection fails
            }
        }
        
        // Convert back to Frame
        Frame enhancedFrame = matConverter.convert(outputMat);
        outputMat.release();
        
        return enhancedFrame;
    }
    
    /**
     * Apply skin smoothing to detected faces
     */
    private static void applySkinSmoothing(Mat image, int smoothingLevel) throws Exception {
        // Initialize face detector
        initFaceDetector();
        
        // Detect faces
        RectVector faces = new RectVector();
        
        // Convert to grayscale for face detection
        Mat grayMat = new Mat();
        opencv_imgproc.cvtColor(image, grayMat, opencv_imgproc.COLOR_BGR2GRAY);
        
        // Detect faces
        faceDetector.detectMultiScale(grayMat, faces);
        grayMat.release();
        
        // Process each detected face
        for (long i = 0; i < faces.size(); i++) {
            Rect faceRect = faces.get(i);
            
            // Expand face region slightly to include entire face
            int expandPixels = (int)(Math.min(faceRect.width(), faceRect.height()) * 0.1);
            int x = Math.max(0, faceRect.x() - expandPixels);
            int y = Math.max(0, faceRect.y() - expandPixels);
            int width = Math.min(image.cols() - x, faceRect.width() + expandPixels * 2);
            int height = Math.min(image.rows() - y, faceRect.height() + expandPixels * 2);
            
            Rect expandedFaceRect = new Rect(x, y, width, height);
            
            // Extract face region
            Mat faceRegion = new Mat(image, expandedFaceRect);
            
            // Apply bilateral filter for skin smoothing while preserving edges
            // Parameters: src, dst, d (diameter), sigmaColor, sigmaSpace
            // Higher smoothingLevel = stronger effect
            int diameter = 7 + smoothingLevel / 3;  // Adjust based on smoothing level
            double sigmaColor = 20 + smoothingLevel * 3;  // Color-based smoothing intensity
            double sigmaSpace = 5 + smoothingLevel;  // Spatial smoothing intensity
            
            Mat smoothedFace = new Mat();
            opencv_imgproc.bilateralFilter(faceRegion, smoothedFace, diameter, sigmaColor, sigmaSpace);
            
            // Copy smoothed face back to the original image
            smoothedFace.copyTo(new Mat(image, expandedFaceRect));
            
            // Clean up
            faceRegion.release();
            smoothedFace.release();
        }
    }
}