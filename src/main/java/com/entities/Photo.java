package com.entities;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class Photo {
    private Frame originalFrame;
    private Frame processedFrame;
    private String fileName;
    private LocalDateTime uploadTime;
    private int width;
    private int height;
    private File sourceFile;
    
    // Flag to track if we're using a downscaled version
    private boolean isDownscaled = false;
    
    // Flag to protect against double freeing resources
    private final AtomicBoolean resourcesFreed = new AtomicBoolean(false);
    
    private static final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    
    // Maximum dimensions for original image (adjust as needed)
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;
    
    // Maximum memory size threshold (in bytes)
    private static final long MAX_MEMORY_SIZE = 10_000_000; // 10 MB
    
    public Photo(Frame frame, String fileName, File sourceFile) {
        try {
            this.fileName = fileName;
            this.uploadTime = LocalDateTime.now();
            this.sourceFile = sourceFile;
            
            // Safely load the frame with memory checks
            loadFrameWithMemoryCheck(frame);
        } catch (Exception e) {
            System.err.println("Error creating Photo from frame: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Photo", e);
        }
    }
    
    public Photo(Frame originalFrame) {
        try {
            // Safely load the frame with memory checks
            loadFrameWithMemoryCheck(originalFrame);
        } catch (Exception e) {
            System.err.println("Error creating Photo from frame: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Photo", e);
        }
    }
    
    private void loadFrameWithMemoryCheck(Frame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("Frame cannot be null");
        }
        
        // Log frame info
        System.out.println("Load Frame - Image Dimensions:");
        System.out.println("Width: " + frame.imageWidth);
        System.out.println("Height: " + frame.imageHeight);
        System.out.println("Channels: " + frame.imageChannels);
        
        long estimatedMemorySize = calculateMemorySize(frame);
        System.out.println("Estimated Memory Size: " + estimatedMemorySize + " bytes");
        
        if (estimatedMemorySize > MAX_MEMORY_SIZE || 
            frame.imageWidth > MAX_WIDTH || 
            frame.imageHeight > MAX_HEIGHT) {
            
            System.out.println("Image exceeds maximum size. Creating downscaled version...");
            isDownscaled = true;
            
            try {
                // Convert Frame to Mat for resizing
                Mat originalMat = matConverter.convert(frame);
                if (originalMat == null || originalMat.isNull()) {
                    throw new RuntimeException("Failed to convert frame to Mat");
                }
                
                Mat resizedMat = new Mat();
                
                // Calculate new dimensions while maintaining aspect ratio
                double scaleFactor = Math.min(
                    (double)MAX_WIDTH / frame.imageWidth,
                    (double)MAX_HEIGHT / frame.imageHeight
                );
                
                // Check if we need to scale more for memory constraints
                if ((long)(frame.imageWidth * scaleFactor) * 
                    (frame.imageHeight * scaleFactor) * 
                    frame.imageChannels > MAX_MEMORY_SIZE) {
                    
                    double memoryScaleFactor = Math.sqrt(
                        (double)MAX_MEMORY_SIZE / 
                        (frame.imageWidth * frame.imageHeight * frame.imageChannels)
                    );
                    
                    scaleFactor = Math.min(scaleFactor, memoryScaleFactor);
                }
                
                int newWidth = Math.max(1, (int)(frame.imageWidth * scaleFactor));
                int newHeight = Math.max(1, (int)(frame.imageHeight * scaleFactor));
                
                System.out.println("Resizing to: " + newWidth + "x" + newHeight);
                
                // Resize the image
                opencv_imgproc.resize(
                    originalMat, 
                    resizedMat, 
                    new org.bytedeco.opencv.opencv_core.Size(newWidth, newHeight)
                );
                
                if (resizedMat == null || resizedMat.isNull()) {
                    throw new RuntimeException("Resizing operation failed");
                }
                
                // Convert back to Frame
                Frame resizedFrame = matConverter.convert(resizedMat);
                
                if (resizedFrame == null) {
                    throw new RuntimeException("Failed to convert Mat back to Frame");
                }
                
                // Store only the resized version - don't keep the original to save memory
                this.originalFrame = resizedFrame.clone();
                this.processedFrame = resizedFrame.clone();
                
                // Clean up native resources
                originalMat.close();
                resizedMat.close();
                
                System.out.println("Successfully created downscaled version");
            } catch (Exception e) {
                System.err.println("Error during image scaling: " + e.getMessage());
                e.printStackTrace();
                
                // Fallback to original frame if resizing fails
                this.originalFrame = frame.clone();
                this.processedFrame = frame.clone();
                isDownscaled = false;
            }
        } else {
            // If image is within limits, use as is
            try {
                this.originalFrame = frame.clone();
                this.processedFrame = frame.clone();
                isDownscaled = false;
            } catch (OutOfMemoryError e) {
                System.err.println("Out of memory while cloning frame: " + e.getMessage());
                // Try a more memory-efficient approach - don't clone, just reference
                this.originalFrame = frame;
                this.processedFrame = frame;
                isDownscaled = false;
            }
        }
        
        updateDimensions();
    }

    private long calculateMemorySize(Frame frame) {
        try {
            return (long)frame.imageWidth * frame.imageHeight * frame.imageChannels;
        } catch (Exception e) {
            System.err.println("Error calculating memory size: " + e.getMessage());
            return Long.MAX_VALUE; // Assume worst case if calculation fails
        }
    }

    public Photo(Photo other) {
        try {
            // Log image dimensions
            System.out.println("Copy constructor - Original Image Dimensions:");
            System.out.println("Width: " + (other.originalFrame != null ? other.originalFrame.imageWidth : "N/A"));
            System.out.println("Height: " + (other.originalFrame != null ? other.originalFrame.imageHeight : "N/A"));
            
            // Copy metadata
            this.fileName = other.fileName;
            this.uploadTime = other.uploadTime;
            this.sourceFile = other.sourceFile;
            this.isDownscaled = other.isDownscaled;
            
            // Safe copying of frames
            try {
                if (other.originalFrame != null) {
                    long estimatedMemorySize = calculateMemorySize(other.originalFrame);
                    System.out.println("Copy constructor - Estimated Memory Size: " + estimatedMemorySize + " bytes");
                    
                    // For large images, don't clone - use reference 
                    if (estimatedMemorySize > MAX_MEMORY_SIZE / 2) {
                        System.out.println("Large image detected in copy constructor, using reference instead of cloning");
                        this.originalFrame = other.originalFrame;
                        this.processedFrame = other.processedFrame;
                    } else {
                        // Safe to clone
                        this.originalFrame = other.originalFrame.clone();
                        this.processedFrame = other.processedFrame.clone();
                    }
                }
            } catch (OutOfMemoryError e) {
                System.err.println("Out of memory in copy constructor: " + e.getMessage());
                // Fallback to referencing instead of cloning
                this.originalFrame = other.originalFrame;
                this.processedFrame = other.processedFrame;
            }
            
            this.width = other.width;
            this.height = other.height;
            
            updateDimensions();
        } catch (Exception e) {
            System.err.println("Error in Photo copy constructor: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Photo copy", e);
        }
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
    
    public void setProcessedFrame(Frame newProcessedFrame) {
        // First release the old processed frame (if it's different from original)
        if (processedFrame != null && processedFrame != originalFrame) {
            try {
                // Mark as processed
                processedFrame = null;
                // Force GC to reclaim memory
                System.gc();
            } catch (Exception e) {
                System.err.println("Error releasing old processed frame: " + e.getMessage());
            }
        }
        
        // Set the new processed frame
        this.processedFrame = newProcessedFrame;
        updateDimensions();
    }
    
    public BufferedImage getProcessedBufferedImage() {
        if (processedFrame == null) {
            System.err.println("Warning: Processed frame is null, cannot convert to BufferedImage");
            return null;
        }
        try {
            return java2DConverter.convert(processedFrame);
        } catch (Exception e) {
            System.err.println("Error converting processed frame to BufferedImage: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public BufferedImage getOriginalBufferedImage() {
        if (originalFrame == null) {
            System.err.println("Warning: Original frame is null, cannot convert to BufferedImage");
            return null;
        }
        
        try {
            return java2DConverter.convert(originalFrame);
        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory converting original frame: " + e.getMessage());
            
            // If we run out of memory and have a processed frame, return that instead
            if (processedFrame != null && processedFrame != originalFrame) {
                System.out.println("Returning processed image instead due to memory constraints");
                return getProcessedBufferedImage();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error converting original frame to BufferedImage: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public Mat getProcessedMat() {
        if (processedFrame == null) {
            System.err.println("Warning: Processed frame is null, cannot convert to Mat");
            return null;
        }
        
        try {
            return matConverter.convert(processedFrame);
        } catch (Exception e) {
            System.err.println("Error converting processed frame to Mat: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
    
    public boolean isDownscaled() {
        return isDownscaled;
    }
    
    /**
     * Releases all resources held by this Photo instance.
     * This should be called when the Photo is no longer needed.
     */
    public void freeResources() {
        // Use atomic operation to ensure we only free once
        if (resourcesFreed.compareAndSet(false, true)) {
            try {
                // Free frames explicitly
                if (originalFrame != null) {
                    originalFrame = null;
                }
                
                if (processedFrame != null) {
                    processedFrame = null;
                }
                
                System.gc();
                System.out.println("Resources freed for photo: " + (fileName != null ? fileName : "unnamed"));
            } catch (Exception e) {
                System.err.println("Error freeing resources: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Frees the original frame to save memory if it's different from the processed frame.
     * Call this when only the processed version is needed.
     */
    public void freeOriginalFrame() {
        if (originalFrame != null && processedFrame != null && originalFrame != processedFrame) {
            try {
                originalFrame = null;
                System.gc();
                System.out.println("Original frame freed for photo: " + (fileName != null ? fileName : "unnamed"));
            } catch (Exception e) {
                System.err.println("Error freeing original frame: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called when this object is about to be garbage collected.
     * This is a backup mechanism to release resources if freeResources() wasn't called.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!resourcesFreed.get()) {
                System.out.println("Warning: Photo being finalized without explicit resource freeing");
                freeResources();
            }
        } finally {
            super.finalize();
        }
    }
}