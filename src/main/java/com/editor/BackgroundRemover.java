package com.editor;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;


import java.util.List;

import org.bytedeco.opencv.opencv_core.*;


import com.entities.BackgroundSettings;

/**
 * 
 * Implements algorithms for background removal and replacement.
 */
public class BackgroundRemover implements ImageProcessor {
    
    private BackgroundSettings settings;
    
    public BackgroundRemover() {
        this.settings = new BackgroundSettings();
    }
    
    public BackgroundRemover(BackgroundSettings settings) {
        this.settings = settings;
    }
    
    @Override
    public Mat process(Mat inputImage) {
        // Default implementation uses GrabCut for automatic background removal
        return removeBackground(inputImage);
    }
    
    /**
     * Automatic background removal using GrabCut algorithm.
     */
    public Mat removeBackground(Mat inputImage) {
        // Create copies of input image for processing
        Mat image = inputImage.clone();
        
        // Convert to grayscale for processing
        Mat gray = new Mat();
        cvtColor(image, gray, COLOR_BGR2GRAY);
        
        // Create mask initialized with possible background and foreground
        Mat mask = new Mat(image.size(), CV_8UC1, Scalar.all(GC_PR_BGD));
        
        // Initialize foreground and background models
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        
        // Define a rectangle that likely contains the person (center of the image)
        Rect rect = new Rect(
            image.cols() / 4, 
            image.rows() / 4, 
            image.cols() / 2, 
            image.rows() / 2
        );
        
        // Apply GrabCut algorithm
        grabCut(image, mask, rect, bgModel, fgModel, 5, GC_INIT_WITH_RECT);
        
        // Create binary mask where foreground is white and background is black
        Mat binaryMask = new Mat();
        compare(mask, new Mat(new Scalar(GC_PR_FGD)), binaryMask, CMP_EQ);
        
        // Apply mask to original image
        Mat result = new Mat(image.size(), image.type(), new Scalar(0, 0, 0, 0));
        image.copyTo(result, binaryMask);
        
        // Apply background replacement if specified
        if (settings.isUseCustomBackground() && settings.getBackgroundImage() != null) {
            result = replaceBackgroundWithImage(result, settings.getBackgroundImage());
        } else {
            result = replaceBackground(result, settings.getBackgroundColor());
        }
        
        return result;
    }
    
    /**
     * Semi-automatic background removal with user input.
     */
    public Mat removeBackgroundInteractive(Mat inputImage, List<Point> foregroundPoints, List<Point> backgroundPoints) {
        Mat image = inputImage.clone();
        
        // Create mask initialized with possible background and foreground
        Mat mask = new Mat(image.size(), CV_8UC1, Scalar.all(GC_PR_BGD));
        
        // Mark foreground and background regions based on user input
        for (Point p : foregroundPoints) {
            int x = (int) p.x();
            int y = (int) p.y();
            if (x >= 0 && x < mask.cols() && y >= 0 && y < mask.rows()) {
                mask.ptr(y, x).put((byte) GC_FGD);

            }
        }
        
        for (Point p : backgroundPoints) {
            int x = (int) p.x();
            int y = (int) p.y();
            if (x >= 0 && x < mask.cols() && y >= 0 && y < mask.rows()) {
                mask.ptr(y, x).put((byte) GC_FGD);

            }
        }
        
        // Initialize foreground and background models
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        
        // Initialize with mask instead of rectangle
        grabCut(image, mask, new Rect(), bgModel, fgModel, 5, GC_INIT_WITH_MASK);
        
        // Create binary mask where foreground is white and background is black
        Mat binaryMask = new Mat();
        compare(mask, new Mat(new Scalar(GC_PR_FGD)), binaryMask, CMP_EQ);
        
        // Apply mask to original image
        Mat result = new Mat(image.size(), image.type(), new Scalar(0, 0, 0, 0));
        image.copyTo(result, binaryMask);
        
        // Apply background replacement
        if (settings.isUseCustomBackground() && settings.getBackgroundImage() != null) {
            result = replaceBackgroundWithImage(result, settings.getBackgroundImage());
        } else {
            result = replaceBackground(result, settings.getBackgroundColor());
        }
        
        return result;
    }
    
    /**
     * Replace background with solid color.
     */
    public Mat replaceBackground(Mat foregroundImage, Scalar backgroundColor) {
        // Create a mask of non-black pixels (foreground)
        Mat mask = new Mat();
        cvtColor(foregroundImage, mask, COLOR_BGR2GRAY);
        threshold(mask, mask, 1, 255, THRESH_BINARY);
        
        // Create background of selected color
        Mat background = new Mat(foregroundImage.size(), foregroundImage.type(), backgroundColor);
        
        // Combine the foreground with the new background
        Mat result = new Mat();
        background.copyTo(result);
        foregroundImage.copyTo(result, mask);
        
        return result;
    }
    
    /**
     * Replace background with custom image.
     */
    public Mat replaceBackgroundWithImage(Mat foregroundImage, Mat backgroundImage) {
        // Resize background image to match foreground dimensions
        Mat resizedBackground = new Mat();
        resize(backgroundImage, resizedBackground, foregroundImage.size());
        
        // Create a mask of non-black pixels (foreground)
        Mat mask = new Mat();
        cvtColor(foregroundImage, mask, COLOR_BGR2GRAY);
        threshold(mask, mask, 1, 255, THRESH_BINARY);
        
        // Combine the foreground with the new background
        Mat result = new Mat();
        resizedBackground.copyTo(result);
        foregroundImage.copyTo(result, mask);
        
        return result;
    }
    
    // Getters and setters
    public BackgroundSettings getSettings() {
        return settings;
    }
    
    public void setSettings(BackgroundSettings settings) {
        this.settings = settings;
    }
}