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

public class ImageUtils {
    private static final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    
    public static BufferedImage frameToBufferedImage(Frame frame) {
        if (frame == null) {
            return null;
        }
        return java2DConverter.convert(frame);
    }
    
    public static Frame bufferedImageToFrame(BufferedImage image) {
        if (image == null) {
            return null;
        }
        return java2DConverter.convert(image);
    }
    
    public static Mat frameToMat(Frame frame) {
        if (frame == null) {
            return null;
        }
        return matConverter.convert(frame);
    }
    
    public static Frame matToFrame(Mat mat) {
        if (mat == null) {
            return null;
        }
        return matConverter.convert(mat);
    }
    
    public static Frame detectFaces(Frame frame) {
        Mat image = frameToMat(frame);
        
        // Convert to grayscale for face detection
        Mat grayImage = new Mat();
        opencv_imgproc.cvtColor(image, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
        
        // Use a pre-trained face detector
        String cascadePath = "./facialDetectionResources/haarcascade_frontalface_default.xml";
        
        try {
            CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);
            RectVector faceDetections = new RectVector();
            
            // Detect faces
            faceDetector.detectMultiScale(grayImage, faceDetections);
            
            // Draw rectangles around detected faces
            for (int i = 0; i < faceDetections.size(); i++) {
                Rect face = faceDetections.get(i);
                opencv_imgproc.rectangle(image, face, new Scalar(0, 255, 0, 255), 2, opencv_imgproc.LINE_8, 0);
            }
            
            grayImage.release();
            faceDetections.deallocate();
            
            return matToFrame(image);
        } catch (Exception e) {
            System.err.println("Error in face detection: " + e.getMessage());
            grayImage.release();
            return frame; // Return original frame if face detection fails
        }
    }
    
    public static int[] calculateHistogram(Mat image) {
        // Convert to HSV
        Mat hsv = new Mat();
        opencv_imgproc.cvtColor(image, hsv, opencv_imgproc.COLOR_BGR2HSV);
        
        // Prepare arguments for calcHist - use MatVector instead of Mat[]
        MatVector channels = new MatVector(3);
        opencv_core.split(hsv, channels);
        
        // Calculate histogram for Hue channel
        Mat hist = new Mat();
        
        // Create pointers for calcHist parameters
        IntPointer channelsPtr = new IntPointer(1).put(0);
        IntPointer histSizePtr = new IntPointer(1).put(180);
        FloatPointer rangesPtr = new FloatPointer(2).put(0.0f).put(180.0f);
        
        // Call calcHist with proper JavaCV parameters
        opencv_imgproc.calcHist(channels.get(0), 1, channelsPtr, new Mat(), hist, 1, histSizePtr, rangesPtr);
        
        // Convert to array - use FloatBuffer to get data from Mat
        int[] histArray = new int[180];
        FloatBuffer histBuffer = hist.createBuffer();
        
        for (int i = 0; i < 180; i++) {
            histArray[i] = (int) histBuffer.get(i);
        }
        
        // Clean up resources
        hsv.release();
        channels.deallocate();
        hist.release();
        channelsPtr.deallocate();
        histSizePtr.deallocate();
        rangesPtr.deallocate();
        
        return histArray;
    }
}
