package com.entities;

import java.awt.Color;

public class BackgroundSettings {
    public enum BackgroundType {
        SOLID_COLOR,
        CUSTOM_IMAGE
    }
    
    private BackgroundType type;
    private Color backgroundColor;
    private String backgroundImagePath;
    private int blurRadius;
    private double edgeThreshold;
    private int iterations;
    
    public BackgroundSettings() {
        // Default settings
        this.type = BackgroundType.SOLID_COLOR;
        this.backgroundColor = Color.WHITE;
        this.blurRadius = 3;
        this.edgeThreshold = 0.5;
        this.iterations = 5;
    }
    
    public BackgroundType getType() {
        return type;
    }
    
    public void setType(BackgroundType type) {
        this.type = type;
    }
    
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public String getBackgroundImagePath() {
        return backgroundImagePath;
    }
    
    public void setBackgroundImagePath(String backgroundImagePath) {
        this.backgroundImagePath = backgroundImagePath;
    }
    
    public int getBlurRadius() {
        return blurRadius;
    }
    
    public void setBlurRadius(int blurRadius) {
        this.blurRadius = blurRadius;
    }
    
    public double getEdgeThreshold() {
        return edgeThreshold;
    }
    
    public void setEdgeThreshold(double edgeThreshold) {
        this.edgeThreshold = edgeThreshold;
    }
    
    public int getIterations() {
        return iterations;
    }
    
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
}
