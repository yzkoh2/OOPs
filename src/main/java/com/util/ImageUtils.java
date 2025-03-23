package com.util;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.FloatPointer;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.io.Closeable;

public class ImageUtils {
    // Use try-with-resources compatible converters
    private static final ThreadLocal<Java2DFrameConverter> java2DConverter = 
        ThreadLocal.withInitial(Java2DFrameConverter::new);
    private static final ThreadLocal<OpenCVFrameConverter.ToMat> matConverter = 
        ThreadLocal.withInitial(OpenCVFrameConverter.ToMat::new);
    
    // Utility method to safely close resources
    private static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("Error closing resource: " + e.getMessage());
                }
            }
        }
    }
    
    public static BufferedImage frameToBufferedImage(Frame frame) {
        if (frame == null) {
            return null;
        }
        return java2DConverter.get().convert(frame);
    }
    
    public static Frame bufferedImageToFrame(BufferedImage image) {
        if (image == null) {
            return null;
        }
        return java2DConverter.get().convert(image);
    }
    
    public static Mat frameToMat(Frame frame) {
        if (frame == null) {
            return null;
        }
        return matConverter.get().convert(frame);
    }
    
    public static Frame matToFrame(Mat mat) {
        if (mat == null) {
            return null;
        }
        return matConverter.get().convert(mat);
    }
    
    public static Frame detectFaces(Frame frame) {
        Mat image = null;
        Mat grayImage = null;
        CascadeClassifier faceDetector = null;
        RectVector faceDetections = null;
        
        try {
            // Convert frame to Mat
            image = frameToMat(frame);
            if (image == null) {
                return frame;
            }
            
            // Convert to grayscale for face detection
            grayImage = new Mat();
            opencv_imgproc.cvtColor(image, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
            
            // Use a pre-trained face detector
            String cascadePath = "./facialDetectionResources/haarcascade_frontalface_default.xml";
            
            faceDetector = new CascadeClassifier(cascadePath);
            faceDetections = new RectVector();
            
            // Detect faces
            faceDetector.detectMultiScale(grayImage, faceDetections);
            
            // Draw rectangles around detected faces
            for (int i = 0; i < faceDetections.size(); i++) {
                Rect face = faceDetections.get(i);
                opencv_imgproc.rectangle(image, face, new Scalar(0, 255, 0, 255), 2, opencv_imgproc.LINE_8, 0);
            }
            
            // Convert back to frame
            return matToFrame(image);
        } catch (Exception e) {
            System.err.println("Error in face detection: " + e.getMessage());
            return frame; // Return original frame if face detection fails
        } finally {
            // Explicitly release all OpenCV resources
            closeQuietly(image, grayImage, faceDetector, faceDetections);
        }
    }
    
    public static int[] calculateHistogram(Mat image) {
        Mat hsv = null;
        MatVector channels = null;
        Mat hist = null;
        IntPointer channelsPtr = null;
        IntPointer histSizePtr = null;
        FloatPointer rangesPtr = null;
        
        try {
            // Convert to HSV
            hsv = new Mat();
            opencv_imgproc.cvtColor(image, hsv, opencv_imgproc.COLOR_BGR2HSV);
            
            // Prepare arguments for calcHist - use MatVector instead of Mat[]
            channels = new MatVector(3);
            opencv_core.split(hsv, channels);
            
            // Calculate histogram for Hue channel
            hist = new Mat();
            
            // Create pointers for calcHist parameters
            channelsPtr = new IntPointer(1).put(0);
            histSizePtr = new IntPointer(1).put(180);
            rangesPtr = new FloatPointer(2).put(0.0f).put(180.0f);
            
            // Call calcHist with proper JavaCV parameters
            opencv_imgproc.calcHist(channels.get(0), 1, channelsPtr, new Mat(), hist, 1, histSizePtr, rangesPtr);
            
            // Convert to array - use FloatBuffer to get data from Mat
            int[] histArray = new int[180];
            FloatBuffer histBuffer = hist.createBuffer();
            
            for (int i = 0; i < 180; i++) {
                histArray[i] = (int) histBuffer.get(i);
            }
            
            return histArray;
        } finally {
            // Explicitly release all OpenCV resources
            closeQuietly(hsv, channels, hist);
            
            // Close pointers separately as they might not implement AutoCloseable
            if (channelsPtr != null) channelsPtr.deallocate();
            if (histSizePtr != null) histSizePtr.deallocate();
            if (rangesPtr != null) rangesPtr.deallocate();
        }
    }
}