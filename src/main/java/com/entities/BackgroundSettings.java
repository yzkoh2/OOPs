package com.entities;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

/**
 * Stores settings for background replacement operations.
 */
public class BackgroundSettings {
    private boolean useCustomBackground;
    private Scalar backgroundColor;
    private Mat backgroundImage;
    
    public BackgroundSettings() {
        // Default is a solid white background
        this.useCustomBackground = false;
        this.backgroundColor = new Scalar(255, 255, 255, 255);
        this.backgroundImage = null;
    }
    
    // Getters and setters
    public boolean isUseCustomBackground() {
        return useCustomBackground;
    }
    
    public void setUseCustomBackground(boolean useCustomBackground) {
        this.useCustomBackground = useCustomBackground;
    }
    
    public Scalar getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(Scalar backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public Mat getBackgroundImage() {
        return backgroundImage;
    }
    
    public void setBackgroundImage(Mat backgroundImage) {
        this.backgroundImage = backgroundImage;
        this.useCustomBackground = (backgroundImage != null);
    }
}