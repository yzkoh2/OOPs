package com.entities;

import org.bytedeco.opencv.opencv_core.Mat;

/**
 * Represents a photo with its associated image data and metadata.
 */
public class Photo {
    private Mat image;
    private String filename;
    private Mat originalImage; // Keep original for reset functionality
    private boolean backgroundRemoved;
    private boolean cropped;

    public Photo() {}

    
    public Photo(Mat image, String filename) {
        this.image = image.clone();
        this.originalImage = image.clone();
        this.filename = filename;
        this.backgroundRemoved = false;
        this.cropped = false;
    }
    
    // Getters and setters
    public Mat getImage() {
        return image;
    }
    
    public void setImage(Mat image) {
        this.image = image;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public Mat getOriginalImage() {
        return originalImage;
    }
    
    public boolean isBackgroundRemoved() {
        return backgroundRemoved;
    }
    
    public void setBackgroundRemoved(boolean backgroundRemoved) {
        this.backgroundRemoved = backgroundRemoved;
    }
    
    public boolean isCropped() {
        return cropped;
    }
    
    public void setCropped(boolean cropped) {
        this.cropped = cropped;
    }
    
    public void resetToOriginal() {
        this.image = originalImage.clone();
        this.backgroundRemoved = false;
        this.cropped = false;
    }
}