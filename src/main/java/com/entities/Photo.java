package com.entities;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;

public class Photo implements Cloneable {
    private Frame originalFrame;
    private Frame processedFrame;
    private String fileName;
    private LocalDateTime uploadTime;
    private int width;
    private int height;
    private File sourceFile;
    
    private static final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    
    public Photo(Frame frame, String fileName, File sourceFile) {
        this.originalFrame = frame;
        this.processedFrame = frame.clone();
        this.fileName = fileName;
        this.uploadTime = LocalDateTime.now();
        this.sourceFile = sourceFile;
        updateDimensions();
    }
    
    private void updateDimensions() {
        if (processedFrame != null) {
            width = processedFrame.imageWidth;
            height = processedFrame.imageHeight;
        }
    }
    
    public Frame getOriginalFrame() {
        return originalFrame;
    }
    
    public Frame getProcessedFrame() {
        return processedFrame;
    }
    
    public void setProcessedFrame(Frame processedFrame) {
        this.processedFrame = processedFrame;
        updateDimensions();
    }
    
    public BufferedImage getProcessedBufferedImage() {
        return java2DConverter.convert(processedFrame);
    }
    
    public BufferedImage getOriginalBufferedImage() {
        return java2DConverter.convert(originalFrame);
    }
    
    public Mat getProcessedMat() {
        return matConverter.convert(processedFrame);
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public File getSourceFile() {
        return sourceFile;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    /**
     * Creates a deep copy of the Photo object
     * @return A cloned Photo object with copies of all frames
     */
    @Override
    public Photo clone() {
        try {
            Photo clone = (Photo) super.clone();
            
            // Deep copy frames
            if (this.originalFrame != null) {
                clone.originalFrame = this.originalFrame.clone();
            }
            
            if (this.processedFrame != null) {
                clone.processedFrame = this.processedFrame.clone();
            }
            
            return clone;
        } catch (CloneNotSupportedException e) {
            // This should not happen since we implement Cloneable
            throw new RuntimeException("Failed to clone Photo", e);
        }
    }
    
    /**
     * Resets the processed frame to match the original frame
     */
    public void resetToOriginal() {
        if (originalFrame != null) {
            this.processedFrame = originalFrame.clone();
            updateDimensions();
        }
    }
}